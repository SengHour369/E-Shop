package com.example.eshop.ai.voice;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Instant;
import java.util.Map;

/** Only Google's provisioning endpoint receives the permanent key. Never log provider bodies. */
@Service
public class GeminiVoiceTokenService {
    private final String key;
    private final RestClient client;
    @org.springframework.beans.factory.annotation.Autowired
    public GeminiVoiceTokenService(@Value("${gemini.api-key:}") String key) {
        this.key=key;
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        client=RestClient.builder().requestFactory(factory).baseUrl("https://generativelanguage.googleapis.com/v1alpha").build();
    }
    GeminiVoiceTokenService(String key,RestClient client){this.key=key;this.client=client;}
    public boolean configured() { return key!=null && !key.isBlank(); }
    public void verifyModel(String model) {
        try {
            JsonNode metadata=client.get().uri("/models/"+model).header("x-goog-api-key",key).retrieve().body(JsonNode.class);
            boolean live=false;
            if(metadata!=null)for(var method:metadata.path("supportedGenerationMethods"))
                if("bidiGenerateContent".equals(method.asText()))live=true;
            if(!live)throw new IllegalStateException();
        }catch(Exception e){throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Free AI voice calling is temporarily unavailable.");}
    }
    public String issue(String model, Map<String,Object> config, Instant expires) {
        try {
            JsonNode response=client.post().uri("/auth_tokens").header("x-goog-api-key",key)
                    .body(Map.of("uses",1,"expireTime",expires.toString(),
                            "newSessionExpireTime",Instant.now().plusSeconds(30).toString(),
                            "bidiGenerateContentSetup",Map.of("model","models/"+model,
                                    "generationConfig",Map.of("responseModalities",java.util.List.of("AUDIO")),
                                    "systemInstruction",config.get("systemInstruction"),
                                    "tools",config.get("tools"),
                                    "inputAudioTranscription",Map.of(),"outputAudioTranscription",Map.of())))
                    .retrieve().body(JsonNode.class);
            String name=response==null?"":response.path("name").asText();
            if(name.isBlank()) throw new IllegalStateException();
            return name;
        } catch(Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Free AI voice calling is temporarily unavailable.");
        }
    }
}
