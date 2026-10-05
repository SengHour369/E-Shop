package com.example.eshop.ai.client;

import com.example.eshop.ai.dto.AiIntentResult;
import com.example.eshop.ai.registry.AiToolDefinition;

import java.util.List;

public interface AiProviderClient {

    AiIntentResult detect(String message, List<AiToolDefinition> allowed);
}
