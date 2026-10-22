package com.example.eshop.common.storefront;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** No cross-request cache: disabling guest access takes effect on the next operation. */
@Service
public class StorefrontAccessClient {
    private final RestClient client;
    public StorefrontAccessClient(@Value("${storefront.admin-url:http://localhost:8087}") String url) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    public StorefrontAccess current() {
        try {
            var result = client.get().uri("/api/v1/storefront/access-settings").retrieve().body(StorefrontAccess.class);
            if (result != null) return result;
        } catch (Exception ignored) { }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Storefront settings unavailable");
    }
    public void require(boolean allowed) {
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guest feature is disabled");
    }
}
