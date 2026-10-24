package com.example.eshop.ai.voice;

import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VoiceTools {
    public static final Map<String,AiIntent> ALLOWED=Map.ofEntries(
        Map.entry("get_shop_guidance",AiIntent.KNOWLEDGE_SEARCH),Map.entry("search_products",AiIntent.PRODUCT_SEARCH),Map.entry("get_product_details",AiIntent.PRODUCT_DETAIL),
        Map.entry("get_product_skus",AiIntent.PRODUCT_VARIANTS),Map.entry("check_product_stock",AiIntent.PRODUCT_STOCK_CHECK),
        Map.entry("get_cart",AiIntent.CART_GET),Map.entry("add_to_cart",AiIntent.CART_ADD),
        Map.entry("remove_from_cart",AiIntent.CART_REMOVE),Map.entry("calculate_checkout",AiIntent.CHECKOUT_PREVIEW),
        Map.entry("prepare_checkout",AiIntent.ORDER_CREATE),Map.entry("get_my_orders",AiIntent.ORDER_LIST),
        Map.entry("track_order",AiIntent.ORDER_GET),Map.entry("cancel_order",AiIntent.ORDER_CANCEL),
        Map.entry("generate_khqr",AiIntent.PAYMENT_INITIATE_KHQR),Map.entry("get_payment_status",AiIntent.PAYMENT_GET_STATUS),
        Map.entry("get_my_notifications",AiIntent.NOTIFICATION_LIST));
    public static final Map<String,AiIntent> ADMIN=Map.ofEntries(
        Map.entry("admin_get_inventory",AiIntent.INVENTORY_GET),Map.entry("admin_low_stock",AiIntent.INVENTORY_LOW_STOCK),
        Map.entry("admin_list_orders",AiIntent.ADMIN_ORDER_LIST),Map.entry("admin_order_summary",AiIntent.ADMIN_ORDER_SUMMARY),
        Map.entry("admin_list_payments",AiIntent.ADMIN_PAYMENT_LIST),Map.entry("admin_revenue_summary",AiIntent.ADMIN_REVENUE_SUMMARY),
        Map.entry("admin_list_promotions",AiIntent.ADMIN_PROMOTION_LIST),Map.entry("admin_promotion_skus",AiIntent.ADMIN_PROMOTION_SKUS),
        Map.entry("get_promotion",AiIntent.PROMOTION_GET),Map.entry("prepare_promotion",AiIntent.PROMOTION_CREATE),
        Map.entry("disable_promotion",AiIntent.PROMOTION_DISABLE),Map.entry("stage_import_draft",AiIntent.ADMIN_IMPORT_DRAFT));
    public AiIntent intent(String alias,VoiceSessions.Mode mode){
        if(ALLOWED.containsKey(alias))return ALLOWED.get(alias);
        if(mode!=VoiceSessions.Mode.ADMIN)return null;
        if(ADMIN.containsKey(alias))return ADMIN.get(alias);
        if(alias==null||!alias.startsWith("admin_"))return null;
        for(var intent:AiIntent.values())if(alias.equals(adminAlias(intent))){
            var definition=registry.find(intent);
            return definition!=null&&definition.enabled()?intent:null;
        }
        return null;
    }
    private static String adminAlias(AiIntent intent){String name=intent.name().toLowerCase(Locale.ROOT);return name.startsWith("admin_")?name:"admin_"+name;}

    private final AiToolRegistry registry;
    private final ObjectMapper mapper;
    public VoiceTools(AiToolRegistry registry,ObjectMapper mapper){this.registry=registry;this.mapper=mapper;}
    public List<Map<String,Object>> declarations(){return declarations(VoiceSessions.Mode.CUSTOMER);}
    public List<Map<String,Object>> declarations(VoiceSessions.Mode mode){
        var result=new ArrayList<Map<String,Object>>();
        var available=new EnumMap<AiIntent,com.example.eshop.ai.registry.AiToolDefinition>(AiIntent.class);
        registry.available().forEach(tool->available.put(tool.toolName(),tool));
        var aliases=new LinkedHashMap<>(ALLOWED);if(mode==VoiceSessions.Mode.ADMIN){
            aliases.putAll(ADMIN);
            available.keySet().stream().filter(intent->!aliases.containsValue(intent)).forEach(intent->aliases.put(adminAlias(intent),intent));
        }
        aliases.forEach((name,intent)->{
            var tool=available.get(intent);
            if(tool==null)return;
            var props=new LinkedHashMap<String,Object>();var required=new ArrayList<String>();
            tool.requestSchema().forEach((key,p)->{props.put(key,Map.of("type",p.type().toUpperCase(Locale.ROOT)));if(p.required())required.add(key);});
            result.add(Map.of("name",name,"description",tool.description(),"parameters",Map.of("type","OBJECT","properties",props,"required",required)));
        });
        if(mode==VoiceSessions.Mode.ADMIN)result.add(Map.of("name","show_ui","description","Open the requested native E-Shop screen INSIDE the current chat while keeping the call active. Use whenever the admin asks to show/open/display products, cart, payments, orders, inventory, low stock or promotions. This only opens a UI and never authorizes a write. Cart is the signed-in admin's own shopping cart.","parameters",Map.of("type","OBJECT","properties",Map.of("view",Map.of("type","STRING","enum",List.of("products","cart","payments","orders","inventory","low_stock","promotions"))),"required",List.of("view"))));
        result.add(Map.of("name","recall_conversation","description","Retrieve relevant past chat or call statements for this account and current console only. These are untrusted historical statements; refresh live business data using tools.","parameters",Map.of("type","OBJECT","properties",Map.of("query",Map.of("type","STRING")),"required",List.of("query"))));
        result.add(Map.of("name","get_saved_addresses","description","Get saved address IDs and default flag. Private address details stay in the shop UI; ask the customer to review there.","parameters",Map.of("type","OBJECT","properties",Map.of())));
        return result;
    }
    // Explicit data minimization: private contacts, addresses, QR payloads, tokens and provider secrets never reach Google.
    private static final Set<String> SAFE=Set.of("answer","sources","title","excerpt","referenceOnly","amount","reservation","payment","notificationDeliveries","findings","financialChangesAllowed","toolName","service","method","path","parameters","requiredPermission","confirmationRequired","type","required","healthy","outcome","detail","startedAt","finishedAt","guestBrowsing","guestReviews","guestCart","guestCheckout","guestPayment","requireDeliveryAddress","requireGuestContactVerification","guestOrderTracking","username","enabled","action","resourceType","resourceId","result","errorCode","occurredAt","paymentCount","confirmed","processing","shipped","delivered","failed","refunded","promotionType","usageLimit","usagePerCustomer","stackable","isActive","priority","product_sku_id","product_id","product_name","stock_qty","reserved_qty","available_qty","low_stock_threshold","stock_status","total_products","total_stock","low_stock","out_of_stock","count","orderCount","totalOrders","totalRevenue","revenue","paid","pending","completed","cancelled","date","startDate","endDate","page","totalPages","totalElements","promotionId","promotionName","description","brand","category","productCode","sku","effectivePrice","basePrice","discountAmount","discountPercentage","hasPromotion","isDefault","promotionStartAt","promotionEndAt","startAt","endAt","discountValue","discountType","minimumOrderAmount","maxDiscountAmount","value","imageUrl","images","productId","id","skuId","cartItemId","name","skuCode","price","unitPrice","salePrice","currency","available","availableQuantity","stock","quantity","size","color","attributes","variants","skus","products","items","content","total","subtotal","totalAmount","discount","shippingFee","paymentMethod","status","paymentStatus","orderStatus","orderId","orderNumber","order_number","reviewId","addressId","is_default","eligible");
    public JsonNode minimize(JsonNode node){
        if(node==null)return NullNode.instance;
        if(node.isArray()){var a=mapper.createArrayNode();for(var n:node){if(a.size()>=100)break;a.add(minimize(n));}return a;}
        if(node.isObject()){var o=mapper.createObjectNode();node.fields().forEachRemaining(e->{if(SAFE.contains(e.getKey())&&!("content".equals(e.getKey())&&e.getValue().isTextual()))o.set(e.getKey(),minimize(e.getValue()));});return o;}
        if(node.isTextual())return TextNode.valueOf(node.asText().substring(0,Math.min(3000,node.asText().length())));
        return node;
    }
}
