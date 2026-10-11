package com.example.eshop.ai.controller;

/** Exact social greetings only: never handles a business request or claims an action. */
final class AiQuickReply {
    private AiQuickReply() {}
    static String reply(String message, String language) {
        if (message == null) return null;
        String text = message.strip().toLowerCase(java.util.Locale.ROOT);
        boolean khmer = "km".equals(language) || text.codePoints().anyMatch(c -> c >= 0x1780 && c <= 0x17ff);
        if (text.matches("(hi|hello|hey|good morning|good afternoon|good evening|សួស្តី|ជំរាបសួរ)[!?. ។]*")) {
            return khmer ? "សួស្តី! តើខ្ញុំអាចជួយអ្នកអ្វីបាន? អ្នកអាចសួរអំពីផលិតផល ការបញ្ជាទិញ ឬការទូទាត់។"
                    : "Hello! How can I help? You can ask about products, orders or payments.";
        }
        if (text.matches("(thanks|thank you|អរគុណ|សូមអរគុណ)[!?. ។]*")) {
            return khmer ? "មិនអីទេ! បើអ្នកត្រូវការជំនួយបន្ថែម សូមប្រាប់ខ្ញុំ។" : "You're welcome! Let me know if you need anything else.";
        }
        return null;
    }
}
