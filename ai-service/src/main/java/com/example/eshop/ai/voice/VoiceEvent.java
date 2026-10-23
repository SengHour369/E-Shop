package com.example.eshop.ai.voice;
import java.util.UUID;
/** Event identities are issued by the backend and shared by voice and existing chat renderers. */
public record VoiceEvent(int schemaVersion,String eventId,Type type,UUID conversationId) {
    public enum Type { PRODUCT_SEARCH_RESULTS, PRODUCT_SELECTED, PRODUCT_DETAIL_LOADED, CART_UPDATED,
        CHECKOUT_READY, PAYMENT_QR_READY, PAYMENT_STATUS_UPDATED, ADMIN_RESULT, TOOL_RESULT, ERROR }
}
