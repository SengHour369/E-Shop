package com.example.eshop.ai.controller;
import com.example.eshop.common.security.LivePermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
@RestController
public class AiAdminInfoController {
    private final LivePermissionService permissions;
    private final ObjectMapper mapper;
    private final URI uri;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public AiAdminInfoController(LivePermissionService permissions, ObjectMapper mapper, @Value("${ai.inference-url:http://localhost:8000}") String base) {
        this.permissions = permissions; this.mapper = mapper;
        uri = URI.create(base.replaceAll("/+$", "") + "/api/v1/ai/info");
    }
    @GetMapping("/api/ai/admin/info") public JsonNode info() {
        if (!permissions.current().administrator()) throw new org.springframework.security.access.AccessDeniedException("Administrator access required");
        try {
            var response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException();
            return mapper.readTree(response.body());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Model information is unavailable");
        } catch (Exception unavailable) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Model information is unavailable"); }
    }
}
