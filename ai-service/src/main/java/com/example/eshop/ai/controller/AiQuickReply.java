package com.example.eshop.ai.controller;

/** Exact social greetings only: never handles a business request or claims an action. */
final class AiQuickReply {

    private AiQuickReply() {}

    static String reply(String message, String language) {
        return reply(message,language,false);
    }

    static String reply(String message, String language, boolean administrator) {
        if (message == null) {
            return null;
        }

        String text = message.strip().toLowerCase(java.util.Locale.ROOT);
        boolean khmer = "km".equals(language)
                || text.codePoints().anyMatch(c -> c >= 0x1780 && c <= 0x17ff);

        if (text.matches(
                "(hi|hello|hey|good morning|good afternoon|good evening|សួស្តី|ជំរាបសួរ)[!?. ។]*"
        )) {
            if(administrator)return khmer
                    ? "សួស្តី! ខ្ញុំជាជំនួយការ Admin របស់អ្នក។ តើអ្នកចង់ពិនិត្យផលិតផល ការបញ្ជាទិញ ស្តុក អតិថិជន ការទូទាត់ ឬរបាយការណ៍?"
                    : "Hello! I’m your admin assistant. I can help manage products, orders, inventory, customers, payments, promotions and reports using your authorized shop tools. What would you like to check?";
            return khmer
                    ? "សួស្តី! តើខ្ញុំអាចជួយអ្នកអ្វីបាន? អ្នកអាចសួរអំពីផលិតផល ការបញ្ជាទិញ ឬការទូទាត់។"
                    : "Hello! How can I help? You can ask about products, orders or payments.";
        }

        if(administrator && text.matches("(all|show all|everything|show everything|ទាំងអស់)[!?. ។]*"))return khmer
                ? "តើអ្នកចង់មើលទិន្នន័យផ្នែកណា៖ ផលិតផល ការបញ្ជាទិញ ស្តុក អតិថិជន ការទូទាត់ ឬរបាយការណ៍? សូមជ្រើសរើសមួយ ដើម្បីពិនិត្យទិន្នន័យពិត។"
                : "Which store data would you like to see: products, orders, inventory, customers, payments, promotions or reports? Choose one so I can check the live records. For example: Show all orders or Show low stock.";

        if (text.matches(
                "(thanks|thank you|អរគុណ|សូមអរគុណ)[!?. ។]*"
        )) {
            return khmer
                    ? "មិនអីទេ! បើអ្នកត្រូវការជំនួយបន្ថែម សូមប្រាប់ខ្ញុំ។"
                    : "You're welcome! Let me know if you need anything else.";
        }

        return null;
    }
}