package com.example.eshop.common.security;

import com.example.eshop.common.request.RequestIds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.http.HttpClient;
import java.time.Duration;

/** Fails closed; account state and permissions are never cached across requests. */
@Service
public class LivePermissionService {

    private final String authUrl;
    private volatile RestClient client;

    public LivePermissionService(
            @Value("${authorization.auth-url:http://localhost:8081}") String authUrl) {
        this.authUrl = authUrl;
    }

    private synchronized RestClient client() {
        if (client != null) {
            return client;
        }
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(3));
        client = RestClient.builder()
                .baseUrl(authUrl)
                .requestFactory(factory)
                .build();
        return client;
    }

    public PermissionSnapshot current() {
        long actorId = CurrentActor.userId();
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new AccessDeniedException("Current account unavailable");
        }
        String bearer = attributes.getRequest().getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authentication required");
        }
        try {
            var request = client().get()
                    .uri("/internal/authorization/me")
                    .header("Authorization", bearer)
                    .header(RequestIds.HEADER, RequestIds.current());
            String parent = attributes.getRequest().getHeader("traceparent");
            if (com.example.eshop.common.request.TraceContextFilter.valid(parent)) {
                request.header("traceparent", parent);
            }
            PermissionSnapshot snapshot = request.retrieve()
                    .body(PermissionSnapshot.class);
            if (snapshot == null || snapshot.userId() != actorId) {
                throw new AccessDeniedException("Current account unavailable");
            }
            return snapshot;
        } catch (Exception exception) {
            throw new AccessDeniedException("Current account unavailable");
        }
    }

    public void require(String permission) {
        PermissionSnapshot snapshot = current();
        if (!snapshot.administrator() || !snapshot.permits(permission)) {
            throw new AccessDeniedException("Required permission missing");
        }
    }
}
