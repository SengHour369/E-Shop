package com.example.eshop.ai;
import com.example.eshop.ai.controller.AiConversationRouting;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class AiConversationRoutingTest {
    @Test void greetingsAndCapabilityQuestionsStayConversational() {
        for (String message : new String[]{"Hi!", "Hello! Can you help me manage my shop?", "What can you do?", "How are you?", "សួស្តី"})
            assertThat(AiConversationRouting.isConversation(message)).as(message).isTrue();
    }
    @Test void greetingWithBusinessRequestStillUsesAuthorizedTools() {
        for (String message : new String[]{"Hello, show my orders", "Can you help me cancel order ORD-123?", "Show low stock", "Create a promotion", "Hi! Find phones"})
            assertThat(AiConversationRouting.isConversation(message)).as(message).isFalse();
    }
}