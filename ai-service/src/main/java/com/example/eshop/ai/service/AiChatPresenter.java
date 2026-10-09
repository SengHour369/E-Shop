package com.example.eshop.ai.service;

import com.example.eshop.ai.dto.AiChatResponse;
import com.example.eshop.ai.dto.AiResponse;
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
                case "KnowledgeCard" -> result.data().path("answer").asText(result.message());
                case "ProductList" -> khmer
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
        AiResponse displayed = new AiResponse(result.executionId(), result.requestId(), result.traceId(),
                result.intent(), result.status(), message, result.errorCode(), result.data());
        List<String> suggestions = result.status() == AiExecutionStatus.SUCCESS
                ? List.of(khmer ? "ស្វែងរកផលិតផល" : "Search products") : List.of();
        return new AiChatResponse(conversationId, administrator ? "SUPER_ADMIN" : "CUSTOMER", card, suggestions, displayed);
    }

    private static String card(AiResponse result) {
        if ("CONFIRMATION_REQUIRED".equals(result.errorCode())) {
            return "ConfirmationCard";
        }
        if (result.status() != AiExecutionStatus.SUCCESS || result.intent() == null) {
            return "ErrorCard";
        }
        return switch (result.intent()) {
            case PRODUCT_SEARCH -> "ProductList";
            case PRODUCT_GET, SKU_GET -> "ProductCard";
            case ORDER_GET, MY_ORDER_STATUS, ORDER_CANCEL -> "OrderCard";
            case MY_ORDERS -> "OrderList";
            case MY_PAYMENT_STATUS -> "PaymentStatusCard";
            case MY_RETURNS -> "ReturnStatusCard";
            case MY_NOTIFICATIONS -> "NotificationList";
            case KNOWLEDGE_SEARCH -> "KnowledgeCard";
            case ADMIN_ORDER_LIST, ADMIN_PAYMENT_LIST, ADMIN_RETURN_LIST, ADMIN_USER_LIST, ADMIN_AUDIT_LOG, INVENTORY_GET,
                    INVENTORY_LOW_STOCK, PROMOTION_GET -> "AdminTable";
            case ADMIN_ORDER_SUMMARY, ADMIN_REVENUE_SUMMARY -> "AdminMetricCard";
            default -> "DataCard";
        };
    }
}
