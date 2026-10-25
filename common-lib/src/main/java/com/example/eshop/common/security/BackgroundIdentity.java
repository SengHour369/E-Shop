package com.example.eshop.common.security;

import java.util.Map;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Scoped worker identity. LivePermissionService still verifies the bearer with auth-service. */
public final class BackgroundIdentity {
    private static final ThreadLocal<String> BEARER=new ThreadLocal<>();
    private BackgroundIdentity() {}
    static String bearer(){return BEARER.get();}
    public static <T>T run(long owner,String username,String bearer,Supplier<T> action){
        var previous=SecurityContextHolder.getContext();var oldBearer=BEARER.get();
        var context=SecurityContextHolder.createEmptyContext();
        var authentication=new UsernamePasswordAuthenticationToken(username,null,List.of(new SimpleGrantedAuthority("WORKFLOW_READ")));
        authentication.setDetails(Map.of("userId",owner));context.setAuthentication(authentication);
        try {SecurityContextHolder.setContext(context);BEARER.set(bearer);return action.get();}
        finally {SecurityContextHolder.setContext(previous);if(oldBearer==null)BEARER.remove();else BEARER.set(oldBearer);}
    }
}
