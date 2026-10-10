package com.example.eshop.ai.controller;

import java.util.Locale;

/** Recognizes capability questions without diverting business operation requests. */
public final class AiConversationRouting {
    private AiConversationRouting() {}

    public static boolean isConversation(String message) {
        if (message == null) return false;
        String text = message.trim().toLowerCase(Locale.ROOT);
        if (text.matches("(hi|hello|hey|good morning|good afternoon|good evening|thanks|thank you|សួស្តី|ជំរាបសួរ)[!?. ]*")) return true;
        text = text.replaceFirst("^(hi|hello|hey)[!,.? ]+", "");
        return text.matches("(can|could|will) you help( me)?( (manage|with|run)( my)? (shop|store|business))?[!?. ]*")
                || text.matches("(what can you do|how can you help( me)?|how are you|who are you)[!?. ]*");
    }
}