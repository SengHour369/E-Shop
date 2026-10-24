package com.example.eshop.ai.voice;

import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

/** All devices share atomic reservations. Issued credentials retain their concurrency lease until expiry. */
@Service
@EnableConfigurationProperties(VoiceProperties.class)
public class VoiceSessions {
    static final ZoneId ZONE=ZoneId.of("Asia/Phnom_Penh");
    static final DefaultRedisScript<Long> RESERVE=new DefaultRedisScript<>("""
        if redis.call('EXISTS', KEYS[1]) == 1 then return -1 end
        local used=tonumber(redis.call('GET', KEYS[2]) or '0')
        local reserved=tonumber(redis.call('GET', KEYS[3]) or '0')
        if used+reserved>=tonumber(ARGV[2]) then return -2 end
        redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
        redis.call('INCR', KEYS[3]); redis.call('EXPIREAT', KEYS[3], ARGV[4])
        redis.call('SET', KEYS[4], ARGV[5], 'EX', tonumber(ARGV[3])+60)
        return 1
        """,Long.class);
    static final DefaultRedisScript<Long> ACTIVATE=new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return -1 end
        if redis.call('EXISTS', KEYS[4]) == 1 then return 0 end
        redis.call('SET', KEYS[4], '1', 'EX', ARGV[2])
        local session=redis.call('GET', KEYS[5])
        if session then
          local data=cjson.decode(session); data.status='CONNECTED'
          redis.call('SET', KEYS[5], cjson.encode(data), 'KEEPTTL')
        end
        redis.call('DECR', KEYS[3]); redis.call('INCR', KEYS[2]); redis.call('EXPIREAT', KEYS[2], ARGV[3])
        return 1
        """,Long.class);
    static final DefaultRedisScript<Long> RELEASE=new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        redis.call('DEL', KEYS[1], KEYS[4])
        if tonumber(redis.call('GET', KEYS[3]) or '0')>0 then redis.call('DECR', KEYS[3]) end
        return 1
        """,Long.class);
    public enum Mode { CUSTOMER, ADMIN }
    public record Session(UUID sessionId, UUID conversationId, String language, String status,
                          Instant expiresAt, String day, int maxDurationSeconds,Mode mode) {
        public Session { if(mode==null)mode=Mode.CUSTOMER; }
        public Session(UUID sessionId,UUID conversationId,String language,String status,Instant expiresAt,String day,int maxDurationSeconds){
            this(sessionId,conversationId,language,status,expiresAt,day,maxDurationSeconds,Mode.CUSTOMER);
        }
    }
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final VoiceProperties properties;
    public VoiceSessions(StringRedisTemplate redis,ObjectMapper mapper,VoiceProperties properties) {
        this.redis=redis;this.mapper=mapper;this.properties=properties;
    }
    private String root(){return "ai:voice:{"+CurrentActor.userId()+"}:";}
    private List<String> keys(Session s){return List.of(root()+"active",root()+s.day()+":used",root()+s.day()+":reserved",root()+"session:"+s.sessionId());}
    static String day(Instant now){return now.atZone(ZONE).toLocalDate().toString();}
    static long reset(Instant now){return now.atZone(ZONE).toLocalDate().plusDays(1).atStartOfDay(ZONE).toEpochSecond();}
    public long retryAfterSeconds(){
        try { Long ttl=redis.getExpire(root()+"active");return ttl==null?0:Math.max(0,ttl); }
        catch(Exception e){throw unavailable();}
    }
    public Map<String,Object> usage(){
        try {
            String prefix=root()+day(Instant.now());
            String value=redis.opsForValue().get(prefix+":used");
            String pending=redis.opsForValue().get(prefix+":reserved");
            int used=value==null?0:Integer.parseInt(value), reserved=pending==null?0:Integer.parseInt(pending);
            return Map.of("dailyLimit",properties.maxCallsPerDay(),"usedToday",used,"reservedToday",reserved,
                    "remainingToday",Math.max(0,properties.maxCallsPerDay()-used-reserved),
                    "maxDurationSeconds",properties.maxCallDurationSeconds(),"timezone",ZONE.getId(),"retryAfterSeconds",retryAfterSeconds());
        }catch(Exception e){throw unavailable();}
    }
    public Session reserve(UUID conversation,String language){return reserve(conversation,language,Mode.CUSTOMER);}
    public Session reserve(UUID conversation,String language,Mode mode){
        Instant now=Instant.now();
        Session s=new Session(UUID.randomUUID(),conversation,language,"CREATED",now.plusSeconds(properties.maxCallDurationSeconds()),day(now),properties.maxCallDurationSeconds(),mode);
        try {
            Long result=redis.execute(RESERVE,keys(s),s.sessionId().toString(),""+properties.maxCallsPerDay(),
                    ""+properties.maxCallDurationSeconds(),""+reset(now),mapper.writeValueAsString(s));
            if(result==null)throw unavailable();
            if(result==-1)throw new ResponseStatusException(HttpStatus.CONFLICT,"A voice call is already active.");
            if(result==-2)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Daily free AI call limit reached.");
            return s;
        } catch(ResponseStatusException e){throw e;}catch(Exception e){throw unavailable();}
    }
    public Session owned(UUID id){return owned(id,0);}
    public Session transcriptOwner(UUID id){return owned(id,60);}
    private Session owned(UUID id,int grace){
        try {
            String json=redis.opsForValue().get(root()+"session:"+id);
            if(json==null)throw new ResponseStatusException(HttpStatus.GONE,"Voice session expired or unavailable.");
            Session s=mapper.readValue(json,Session.class);
            if(!s.expiresAt().plusSeconds(grace).isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.GONE,"Voice session expired.");
            return s;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw unavailable();}
    }
    public boolean connected(Session s){
        try {
            var k=new ArrayList<>(keys(s));k.set(3,root()+"connected:"+s.sessionId());k.add(root()+"session:"+s.sessionId());
            Long result=redis.execute(ACTIVATE,k,s.sessionId().toString(),""+properties.maxCallDurationSeconds(),""+reset(Instant.now()));
            if(result==null||result<0)throw unavailable();
            return result==1;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw unavailable();}
    }
    public void provisioningFailed(Session s){
        try {redis.execute(RELEASE,keys(s),s.sessionId().toString());}catch(Exception ignored){/* fail closed: reservation remains */}
    }
    public Session end(Session s){
        // Do not release the provider lease: a copied direct credential cannot be revoked by this endpoint.
        var ended=new Session(s.sessionId(),s.conversationId(),s.language(),"ENDED",s.expiresAt(),s.day(),s.maxDurationSeconds(),s.mode());
        try {redis.opsForValue().set(root()+"session:"+s.sessionId(),mapper.writeValueAsString(ended),Duration.between(Instant.now(),s.expiresAt()).plusSeconds(60));return ended;}
        catch(Exception e){throw unavailable();}
    }
    public Session active(UUID id){Session s=owned(id);if("ENDED".equals(s.status()))throw new ResponseStatusException(HttpStatus.GONE,"Voice call ended.");return s;}
    private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Voice session storage is unavailable.");}
}
