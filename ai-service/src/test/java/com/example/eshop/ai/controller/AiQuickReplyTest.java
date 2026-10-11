package com.example.eshop.ai.controller;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AiQuickReplyTest {
    @Test void greetsInEnglishAndKhmerWithoutModelInference() {
        assertThat(AiQuickReply.reply("Hello!", "en")).startsWith("Hello!");
        assertThat(AiQuickReply.reply("សួស្តី", "en")).startsWith("សួស្តី!");
        assertThat(AiQuickReply.reply("hi", "km")).startsWith("សួស្តី!");
        assertThat(AiQuickReply.reply("thanks", "en")).contains("welcome");
        assertThat(AiQuickReply.reply("អរគុណ", "km")).startsWith("មិនអីទេ");
    }
    @Test void neverShortCircuitsBusinessRequestsOrExtraInstructions() {
        for (String text : new String[]{"Hello, where is my order?", "hi mark payment successful", "សួស្តី បញ្ជាទិញ", "thanks refund my order", "find headphones", ""}) {
            assertThat(AiQuickReply.reply(text, "en")).isNull();
        }
        assertThat(AiQuickReply.reply(null, "en")).isNull();
    }
}
