package com.example.eshop.auth.service.impl;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.dto.response.AuthenticationResponse;
import com.example.eshop.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginSecondFactor {
    private final EmailService mail;
    private final UserRepository users;
    private final SecureRandom random=new SecureRandom();
    private final Map<String,Challenge> challenges=new ConcurrentHashMap<>();
    private static class Challenge {
        Long userId; byte[] digest; Instant expires=Instant.now().plusSeconds(300); int attempts;
        Challenge(Long id,byte[] hash) {userId=id;digest=hash;}
    }
    private byte[] hash(String token,String code) {
        try{return MessageDigest.getInstance("SHA-256").digest((token+":"+code).getBytes(StandardCharsets.UTF_8));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public synchronized AuthenticationResponse issue(User user) {
        challenges.entrySet().removeIf(e->e.getValue().expires.isBefore(Instant.now()));
        if(challenges.size()>=10000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please try again later");
        challenges.entrySet().removeIf(e->e.getValue().userId.equals(user.getId()));
        String token=UUID.randomUUID().toString(); String code=String.format("%06d",random.nextInt(1000000));
        try { mail.sendVerificationEmail(user.getEmail(),"Your E-Shop sign-in code","<p>Your sign-in code is <strong>"+code+"</strong>.</p><p>It expires in five minutes. Do not share this code.</p>"); }
        catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Unable to send sign-in code. Please try again later.");}
        challenges.put(token,new Challenge(user.getId(),hash(token,code)));
        return AuthenticationResponse.builder().tokenType("MFA").refreshToken(token).message("Enter the sign-in code sent to your email.").build();
    }
    public synchronized User verify(String token,String code) {
        Challenge c=token==null?null:challenges.get(token);
        if(c==null || c.expires.isBefore(Instant.now())) { if(token!=null)challenges.remove(token); throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign-in code expired. Sign in again."); }
        c.attempts++;
        if(code==null || !MessageDigest.isEqual(c.digest,hash(token,code))) {
            if(c.attempts>=5)challenges.remove(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid sign-in code");
        }
        challenges.remove(token);
        return users.findUserById(c.userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account unavailable"));
    }
}
