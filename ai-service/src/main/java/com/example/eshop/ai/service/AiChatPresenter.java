package com.example.eshop.ai.service;

import com.example.eshop.ai.dto.AiChatResponse;
import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.ai.dto.ChatPresentation;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.enums.AiExecutionStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Grounded display text. Private backend data is not sent to a text-generation provider. */
@Service
public class AiChatPresenter {

    public AiChatResponse present(UUID conversationId, boolean administrator, String language, AiResponse result) {
        boolean khmer = "km".equals(language);
        String card = card(result);
        String message = result.message();
        if ("CONFIRMATION_REQUIRED".equals(result.errorCode())) {
            message = khmer
                    ? "សូមពិនិត្យសកម្មភាពនេះ ហើយបញ្ជាក់ក្នុងរយៈពេលប្រាំនាទី។"
                    : "Review the action below and confirm within five minutes.";
        } else if (result.status() == AiExecutionStatus.SUCCESS && result.data() != null) {
            message = switch (card) {
                case "ImportDraftCard" -> result.message();
                case "KnowledgeCard" -> result.data().path("answer").asText(result.message());
                case "ProductList" -> result.data().isArray() && result.data().isEmpty()
                        ? (khmer ? "រកមិនឃើញផលិតផលដែលត្រូវគ្នាទេ។ សូមសាកឈ្មោះផ្សេង ឬប្តូរតម្លៃ។"
                                : "No matching products found. Try another name or change the price range.")
                        : khmer
                        ? "នេះជាផលិតផលដែលបានរកឃើញ។ តម្លៃ និងស្តុកមកពីទិន្នន័យបច្ចុប្បន្ន។"
                        : "Here are the matching products, with current backend prices and availability.";
                case "OrderCard", "OrderList" -> khmer
                        ? "នេះជាព័ត៌មានការបញ្ជាទិញរបស់អ្នក។"
                        : "Here is your current order information.";
                case "PaymentStatusCard" -> khmer ? "នេះជាស្ថានភាពការទូទាត់របស់អ្នក។" : "Here is your payment status.";
                case "AdminTable" -> khmer ? "នេះជាទិន្នន័យដែលអ្នកមានសិទ្ធិមើល។" : "Here are the records you are authorized to view.";
                default -> khmer ? "នេះជាលទ្ធផលពីប្រព័ន្ធ E-Shop។" : "Here is the result from E-Shop.";
            };
        }
        if (result.status() == AiExecutionStatus.FAILURE && result.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_SEARCH) {
            message = khmer ? "មិនអាចស្វែងរកផលិតផលបាននៅពេលនេះ។ សូមព្យាយាមម្តងទៀត។"
                    : "Product search is temporarily unavailable. Please try again.";
        }
        AiResponse displayed = new AiResponse(result.executionId(), result.requestId(), result.traceId(),
                result.intent(), result.status(), message, result.errorCode(), result.data());
        List<String> suggestions = result.status() == AiExecutionStatus.SUCCESS
                ? List.of(khmer ? "ស្វែងរកផលិតផល" : "Search products") : List.of();
        // Intent is the registered execution result, not an untrusted model layout.
        // The native screen re-fetches using the caller's JWT and domain authorization.
        ChatPresentation presentation = null;
        if (administrator && result.status() == AiExecutionStatus.SUCCESS && result.intent() != null) {
            presentation = switch (result.intent()) {
                case ADMIN_ORDER_LIST -> ChatPresentation.orders();
                case CART_GET -> ChatPresentation.workspace("cart.checkout");
                case ADMIN_PAYMENT_LIST -> ChatPresentation.workspace("payments.list");
                case INVENTORY_GET -> ChatPresentation.inventory(false);
                case INVENTORY_LOW_STOCK -> ChatPresentation.inventory(true);
                default -> null;
            };
        }
        return new AiChatResponse(conversationId, administrator ? "SUPER_ADMIN" : "CUSTOMER", card, suggestions, displayed, presentation);
    }

    private static String card(AiResponse result) {
        if ("CONFIRMATION_REQUIRED".equals(result.errorCode())) {
            return "ConfirmationCard";
        }
        if (result.status() != AiExecutionStatus.SUCCESS || result.intent() == null) {
            return "ErrorCard";
        }
        return switch (result.intent()) {
            case ADMIN_IMPORT_DRAFT -> "ImportDraftCard";
            case PRODUCT_SEARCH -> "ProductList";
            case PRODUCT_GET, SKU_GET, PRODUCT_DETAIL, PRODUCT_VARIANTS, PRODUCT_STOCK_CHECK -> "ProductCard";
            case CART_GET, CART_ADD, CART_UPDATE, CART_REMOVE, CHECKOUT_PREVIEW -> "CartCard";
            case ORDER_CREATE -> "OrderCard";
            case ORDER_GET, MY_ORDER_STATUS, ORDER_CANCEL -> "OrderCard";
            case MY_ORDERS, ORDER_LIST -> "OrderList";
            case MY_PAYMENT_STATUS, PAYMENT_INITIATE_KHQR, PAYMENT_GET_STATUS -> "PaymentStatusCard";
            case MY_RETURNS, RETURN_GET_STATUS -> "ReturnStatusCard";
            case MY_NOTIFICATIONS, NOTIFICATION_LIST -> "NotificationList";
            case KNOWLEDGE_SEARCH -> "KnowledgeCard";
            case ADMIN_TOOL_LIST, ADMIN_SERVICE_STATUS, ADMIN_STOREFRONT_ACCESS, ADMIN_PROMOTION_LIST, ADMIN_PROMOTION_SKUS, ADMIN_ORDER_LIST, ADMIN_PAYMENT_LIST, ADMIN_RETURN_LIST, ADMIN_USER_LIST, ADMIN_AUDIT_LOG, INVENTORY_GET,
                    INVENTORY_LOW_STOCK, PROMOTION_GET -> "AdminTable";
            case ADMIN_ORDER_SUMMARY, ADMIN_REVENUE_SUMMARY -> "AdminMetricCard";
            default -> "DataCard";
        };
    }
}
