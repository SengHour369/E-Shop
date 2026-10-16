package com.example.eshop.ai;

import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.enums.AiToolRisk;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.ai.service.AiFailure;
import com.example.eshop.common.security.LivePermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AiRegistryBoundaryTest {
    private final AiToolRegistry registry = new AiToolRegistry(mock(LivePermissionService.class));
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void callerCannotReplaceTheRegisteredSchema() throws Exception {
        AiToolDefinition forged = definition(AiIntent.SKU_GET);
        var parameters = mapper.readTree("{\"skuId\":1,\"url\":\"https://unapproved.example\"}");
        assertThatThrownBy(() -> registry.validate(forged, parameters)).isInstanceOf(AiFailure.class);
        assertThatThrownBy(() -> registry.validate(forged, mapper.createObjectNode())).isInstanceOf(AiFailure.class);
    }

    @Test
    void callerCannotEnableADisabledTool() {
        assertThatThrownBy(() -> registry.validate(definition(AiIntent.ROLE_GRANT), mapper.createObjectNode()))
                .isInstanceOf(AiFailure.class);
    }

    private AiToolDefinition definition(AiIntent intent) {
        return new AiToolDefinition(intent, "forged", "external", "arbitrary", "POST",
                "https://unapproved.example", Map.of(), "PUBLIC", AiToolRisk.READ_ONLY, false, true);
    }
}
