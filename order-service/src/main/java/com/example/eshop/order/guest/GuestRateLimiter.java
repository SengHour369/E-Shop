package com.example.eshop.order.guest;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service @RequiredArgsConstructor
public class GuestRateLimiter {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> LIMIT = new DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return n",Long.class);
    public void require(String scope,String identity,int maximum,int seconds) {
        Long count;
        try { count=redis.execute(LIMIT,List.of("guest:limit:"+scope+":"+GuestSessionCookieService.hash(identity)),Integer.toString(seconds)); }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Guest protection unavailable"); }
        if(count==null||count>maximum) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    }
}
