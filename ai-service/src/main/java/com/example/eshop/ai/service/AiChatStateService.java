package com.example.eshop.ai.service;

import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatStateService {

    private static final DefaultRedisScript<Long> RATE_LIMIT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], 60) end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private AiConversationMemory memory;

    public String withContext(UUID conversationId, String message) {
        if (!AiToolRegistry.authenticated()) {
            return message;
        }
        String context = references(conversationId).toString();
        if (context == null || context.equals("{}")) {
            return message;
        }
        return message + "\nPrevious backend result references (fetch current details before answering): " + context;
    }

    public void remember(UUID conversationId, com.example.eshop.ai.dto.AiResponse response) {
        if (!AiToolRegistry.authenticated()
                || response.status() != com.example.eshop.ai.enums.AiExecutionStatus.SUCCESS
                || response.data() == null) {
            return;
        }
        var context = mapper.createObjectNode();
        var removals=new java.util.ArrayList<String>();
        if(response.intent().name().startsWith("CART_"))removals.addAll(List.of("reviewId","reviewedAddressId","reviewedPaymentMethod"));

        var data = response.data();
        if(java.util.Set.of(com.example.eshop.ai.enums.AiIntent.ADMIN_ORDER_LIST,com.example.eshop.ai.enums.AiIntent.ADMIN_PAYMENT_LIST,
                com.example.eshop.ai.enums.AiIntent.ADMIN_RETURN_LIST,com.example.eshop.ai.enums.AiIntent.ADMIN_USER_LIST,
                com.example.eshop.ai.enums.AiIntent.INVENTORY_LOW_STOCK,com.example.eshop.ai.enums.AiIntent.ADMIN_ORDER_SUMMARY,
                com.example.eshop.ai.enums.AiIntent.ADMIN_REVENUE_SUMMARY).contains(response.intent()))context.put("lastReadTool",response.intent().name());
        if (response.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_SEARCH && data.isArray()) {
            var ids = context.putArray("productIdsInDisplayOrder");
            data.forEach(product -> {
                if (product.path("id").isIntegralNumber() && ids.size() < 20) {
                    ids.add(product.path("id").asLong());
                }
            });
            context.set("searchProductIds",ids.deepCopy());
        } else if ((response.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_GET || response.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_DETAIL || response.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_VARIANTS)) {
            context.set("selectedProductId", data.path("id"));
        } else if (response.intent() == com.example.eshop.ai.enums.AiIntent.SKU_GET || response.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_STOCK_CHECK) {
            context.set("selectedSkuId", data.path("id"));
        } else if (response.intent() == com.example.eshop.ai.enums.AiIntent.MY_ORDER_STATUS
                || response.intent() == com.example.eshop.ai.enums.AiIntent.ORDER_GET) {
            context.set("selectedOrderNumber", data.path("orderNumber"));
            if(data.path("id").isIntegralNumber())context.set("selectedOrderId",data.path("id"));
        }
        if(response.intent()==com.example.eshop.ai.enums.AiIntent.CHECKOUT_PREVIEW){
            if(data.path("reviewId").isTextual())context.set("reviewId",data.path("reviewId"));
            if(data.path("addressId").isIntegralNumber())context.set("reviewedAddressId",data.path("addressId"));
            if(data.path("paymentMethod").isTextual())context.set("reviewedPaymentMethod",data.path("paymentMethod"));
        }
        if(response.intent()==com.example.eshop.ai.enums.AiIntent.ORDER_CREATE){
            if(data.path("id").isIntegralNumber())context.set("selectedOrderId",data.path("id"));
            if(data.path("order_number").isTextual())context.set("selectedOrderNumber",data.path("order_number"));
            removals.add("reviewId");
        }
        if(response.intent()==com.example.eshop.ai.enums.AiIntent.PAYMENT_GET_STATUS||response.intent()==com.example.eshop.ai.enums.AiIntent.PAYMENT_INITIATE_KHQR){
            if(data.path("orderId").isIntegralNumber())context.set("selectedOrderId",data.path("orderId"));
        }
        if (!context.isEmpty()||!removals.isEmpty())mergeReferences(conversationId,context,removals);
    }

    private static final DefaultRedisScript<Long> MERGE_CONTEXT=new DefaultRedisScript<>("""
        local old=cjson.decode(redis.call('GET',KEYS[1]) or '{}')
        local changes=cjson.decode(ARGV[1])
        if changes.productIdsInDisplayOrder and not changes.searchProductIds then
            local allowed={}
            for _,value in ipairs(old.searchProductIds or {}) do allowed[value]=true end
            local seen={}
            for _,value in ipairs(changes.productIdsInDisplayOrder) do
                if not allowed[value] or seen[value] then return 0 end
                seen[value]=true
            end
        end
        if changes.selectedProductId and old.selectedProductId~=changes.selectedProductId then old.selectedSkuId=nil end
        for _,key in ipairs(cjson.decode(ARGV[2])) do old[key]=nil end
        for key,value in pairs(changes) do old[key]=value end
        local encoded=cjson.encode(old)
        for _,key in ipairs({'productIdsInDisplayOrder','searchProductIds'}) do
            encoded=string.gsub(encoded,'"'..key..'":{}','"'..key..'":[]')
        end
        redis.call('SET',KEYS[1],encoded,'EX',1800)
        return 1
        """,Long.class);
    private void mergeReferences(UUID id,com.fasterxml.jackson.databind.node.ObjectNode changes,List<String> removals){
        try{Long saved=redis.execute(MERGE_CONTEXT,List.of(conversationKey(id)),changes.toString(),mapper.writeValueAsString(removals));
            if(saved==null)throw new IllegalStateException();
            if(saved==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Display order must contain verified search results.");
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Conversation context is unavailable.");}
    }
    public com.fasterxml.jackson.databind.JsonNode references(UUID id){
        try{
            String json=redis.opsForValue().get(conversationKey(id));
            var context=json==null?mapper.createObjectNode():mapper.readTree(json);
            if(context.isObject()&&context.has("voiceExpiresAt")&&java.time.Instant.parse(context.path("voiceExpiresAt").asText()).isBefore(java.time.Instant.now()))
                ((com.fasterxml.jackson.databind.node.ObjectNode)context).put("voiceCallActive",false);
            return context;
        }catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Conversation context is unavailable.");}
    }
    public void voice(UUID id,UUID session,String language,java.time.Instant expires,boolean active){
        var context=mapper.createObjectNode().put("voiceSessionId",session.toString()).put("preferredLanguage",language)
                .put("voiceCallActive",active).put("voiceExpiresAt",expires.toString());
        mergeReferences(id,context,List.of());
    }
    public void displayedProducts(UUID id,List<Long> ids){
        var allowed=references(id).path("searchProductIds");var set=new java.util.HashSet<Long>();allowed.forEach(value->set.add(value.asLong()));
        if(!set.containsAll(ids)||new java.util.HashSet<>(ids).size()!=ids.size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Display order must contain verified search results.");
        var delta=mapper.createObjectNode();delta.set("productIdsInDisplayOrder",mapper.valueToTree(ids));mergeReferences(id,delta,List.of());
    }

    private static String conversationKey(UUID id) {
        return "ai:conversation:" + CurrentActor.userId() + ":" + id;
    }

    public void rateLimit(String remoteAddress) {
        String actor = AiToolRegistry.authenticated() ? "user:" + CurrentActor.userId() : "ip:" + remoteAddress;
        try {
            Long count = redis.execute(RATE_LIMIT, List.of("ai:rate:" + actor));
            if (count == null) {
                throw new IllegalStateException("Rate limit unavailable");
            }
            if (count > 20) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before sending another message.");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chat is temporarily unavailable.");
        }
    }

    private String dialogueKey(UUID id) {
        String actor = AiToolRegistry.authenticated() ? "user:" + CurrentActor.userId() : "guest";
        return "ai:dialogue:" + actor + ":" + id;
    }

    public java.util.List<java.util.Map<String, String>> dialogue(UUID id) { return dialogue(id,false); }
    public java.util.List<java.util.Map<String, String>> dialogue(UUID id,boolean admin) {
        if(memory!=null)return memory.recent(id,admin);
        if (!AiToolRegistry.authenticated()) return java.util.List.of();
        String json = redis.opsForValue().get(dialogueKey(id));
        if (json == null) return java.util.List.of();
        try {
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception invalid) {
            return java.util.List.of();
        }
    }

    public void rememberDialogue(UUID id, String question, String answer) { rememberDialogue(id,question,answer,false); }
    public void rememberDialogue(UUID id, String question, String answer,boolean admin) {
        if(memory!=null){
            memory.exchange(id,admin,question,answer);
            return;
        }
        if (!AiToolRegistry.authenticated()) return;
        var turns = new java.util.ArrayList<>(dialogue(id));
        turns.add(java.util.Map.of("role", "user", "content", question));
        turns.add(java.util.Map.of("role", "assistant", "content", answer));
        if (turns.size() > 8) turns = new java.util.ArrayList<>(turns.subList(turns.size() - 8, turns.size()));
        try {
            redis.opsForValue().set(dialogueKey(id), mapper.writeValueAsString(turns), Duration.ofMinutes(30));
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
            throw new IllegalStateException("Conversation could not be saved", invalid);
        }
    }
    public UUID conversation(UUID requested) {
        if (!AiToolRegistry.authenticated()) {
            return requested == null ? UUID.randomUUID() : requested;
        }
        UUID id = requested == null ? UUID.randomUUID() : requested;
        String key = conversationKey(id);
        try {
            if (requested != null && !Boolean.TRUE.equals(redis.hasKey(key))) {
                if(memory==null||!memory.owns(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found or expired.");
                redis.opsForValue().set(key,"{}",Duration.ofMinutes(30));
            }
            if (requested == null) {
                redis.opsForValue().set(key, "{}", Duration.ofMinutes(30));
            } else {
                redis.expire(key, Duration.ofMinutes(30));
            }
            return id;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Conversation storage is unavailable.");
        }
    }
}

