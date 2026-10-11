package com.example.eshop.ai.controller;

import com.example.eshop.ai.dto.AiChatResponse;
import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Storefront checkout help. This response does not execute a purchase or payment. */
public final class CustomerPurchaseGuide {
    private CustomerPurchaseGuide() {}
    public static boolean matches(String message) {
        if (message == null) return false;
        String text = message.trim().toLowerCase(Locale.ROOT).replaceAll("[?!.]+$", "").trim();
        return text.matches("(how (do|can) i|help me|can you help me)( (to|with))? (place an order|order|buy|checkout|check out|pay|make a payment|place an order and pay)")
                || text.matches("(i (want|would like) to|please|can you) (place an order|order|buy|checkout|check out|order for me)")
                || text.matches("(what|which) payment (methods|options|providers)( (do you accept|are available|can i use))?")
                || text.equals("how to order") || text.equals("how to pay")
                || text.equals("how do i place an order and pay")
                || text.equals("តើខ្ញុំអាចបញ្ជាទិញនិងបង់ប្រាក់ដោយរបៀបណា")
                || text.equals("របៀបបញ្ជាទិញ") || text.equals("របៀបបង់ប្រាក់");
    }
    public static AiChatResponse response(UUID id, String language) {
        boolean km = "km".equals(language);
        String message = km
                ? "ខ្ញុំអាចជួយអ្នកជ្រើសរើសផលិតផល និងណែនាំការបញ្ជាទិញបាន។ សូមប្រាប់ឈ្មោះផលិតផល ពណ៌ ទំហំ និងចំនួនដែលអ្នកចង់បាន។ បន្ថែមផលិតផលទៅកន្ត្រក រួចចូល Checkout ដើម្បីពិនិត្យផលិតផល ជ្រើសអាសយដ្ឋាន និងវិធីបង់ប្រាក់។ Checkout មាន BAKONG KHQR និងការបង់ប្រាក់ពេលដឹកជញ្ជូន។ សូមពិនិត្យចំនួនទឹកប្រាក់ ហើយចុច Place Order ដោយខ្លួនឯង។ ការបង់ប្រាក់ធ្វើឡើងដាច់ដោយឡែកបន្ទាប់ពីបញ្ជាទិញ។ សូមស្កេន KHQR នៅពេលហាងបានបើកមុខងារនេះ។ ការបង់ប្រាក់ត្រូវបានផ្ទៀងផ្ទាត់ដោយ Bakong។ សូមចូល Payment Information ដើម្បីពិនិត្យប្រវត្តិការបង់ប្រាក់ និងភាពអាចប្រើបាន។ ស្ថានភាពការបង់ប្រាក់ត្រូវបានផ្ទៀងផ្ទាត់ដោយប្រព័ន្ធ។ កុំផ្ញើលេខកាត ពាក្យសម្ងាត់ PIN ឬ OTP ក្នុងឆាត។"
                : "I can help you choose products and guide your order. Tell me the product, variant and quantity you want. Add the selected item to your cart, then sign in and open Checkout. Review your items, choose a saved shipping address and payment provider, check the total, and press Place Order yourself. Checkout offers Bakong KHQR (also scannable in compatible ABA apps) and cash on delivery. Availability depends on shop configuration. Payment is completed separately after placing the order. When enabled, open the order QR and complete payment in your banking app. Placing an order does not charge you. Payment success requires trusted Bakong verification. Open Payment Information to review payment records and current availability. I can check payment status using your payment ID. Never share card details, banking passwords, PINs or OTPs here.";
        return new AiChatResponse(id, "CUSTOMER", "CheckoutGuide",
                List.of(km ? "ស្វែងរកផលិតផល" : "Find products", km ? "ការបញ្ជាទិញរបស់ខ្ញុំ" : "Show my orders"),
                new AiResponse(null, com.example.eshop.common.request.RequestIds.current(), null,
                        AiIntent.UNKNOWN, AiExecutionStatus.SUCCESS, message, null, null));
    }
}