package com.example.eshop.payment.automation;

import com.example.eshop.common.automation.*;
import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.payment.bakong.service.BakongService;
import com.example.eshop.payment.bakong.KhqrType;
import com.example.eshop.payment.bakong.dto.*;
import com.example.eshop.payment.model.*;
import com.example.eshop.payment.repository.*;
import com.example.eshop.payment.enumeration.*;
import com.example.eshop.payment.dto.request.PaymentTransactionRequest;
import com.example.eshop.payment.service.PaymentTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "automation.enabled", havingValue = "true")
public class PaymentAutomation implements CommerceHandler {

    private final PaymentTransactionRepository transactions;
    private final PaymentTransactionStatusHistoryRepository history;
    private final PaymentRepository payments;
    private final PaymentTransactionService transactionService;
    private final OrderAutomationClient orders;
    private final BakongService bakong;
    private final ObjectMapper json;
    private final AutomationEvents events;
    private final com.example.eshop.common.audit.AuditLogService audit;
    private final TransactionTemplate tx;

    @Value("${bakong.account-id}")
    private String recipient;

    @Value("${automation.merchant-name:}")
    private String merchantName;

    @Value("${automation.merchant-id:}")
    private String merchantId;

    @Value("${automation.acquiring-bank:}")
    private String acquiringBank;

    @Value("${bakong.khqr-type:MERCHANT}")
    private KhqrType khqrType = KhqrType.MERCHANT;

    @jakarta.annotation.PostConstruct
    public void validateMerchantConfiguration() {
        if (!khqrType.configured(recipient, merchantName, merchantId, acquiringBank)) {
            throw new IllegalStateException("KHQR account configuration is required; MERCHANT mode also requires registered merchant details");
        }
    }

    @Override
    @Transactional
    public void handle(CommerceEvent event) {
        if (event.type() != CommerceEvent.Type.ORDER_CREATED
                && event.type() != CommerceEvent.Type.ORDER_CANCELLED) {
            return;
        }

        var order = orders.get(event.orderId());

        if (order == null
                || order.orderId() != event.orderId()
                || order.userId() != event.userId()) {
            throw new IllegalStateException("Authoritative order mismatch");
        }

        var existing = transactions.findByOrder(order.orderId())
                .stream()
                .filter(t -> t.getQrMd5() != null)
                .findFirst();

        if (event.type() == CommerceEvent.Type.ORDER_CANCELLED) {
            // Keep querying a cancelled QR: a late transfer becomes a review alert, never a revived order.
            existing.ifPresent(t -> events.review(
                    order.orderId(),
                    "CANCELLED_QR_MONITORING",
                    "An issued QR may still receive a transfer. "
                            + "Reconciliation continues until its expiry."
            ));
            return;
        }

        if (!"PENDING".equals(order.status()) || existing.isPresent()) {
            return;
        }

        if ("CASH_ON_DELIVERY".equals(order.method())) {
            return;
        }

        if (!Set.of("BAKONG", "KHQR", "ABA").contains(order.method())) {
            events.review(
                    order.orderId(),
                    "PAYMENT_METHOD_UNSUPPORTED",
                    "This payment method has no enabled provider automation adapter."
            );
            return;
        }

        validateMerchantConfiguration();

        if (order.amount() == null
                || order.amount().signum() <= 0
                || !"USD".equals(order.currency())) {
            throw new IllegalStateException(
                    "Invalid authoritative order amount"
            );
        }

        // Existing KHQR SDK creates the dynamic QR. Amount and identity never come from the model/customer.
        var generated = json.valueToTree(
                bakong.generateQR(
                        BakongRequest.builder()
                                .currency(order.currency())
                                .amount(order.amount().doubleValue())
                                .merchantName(merchantName)
                                .merchantCity("PHNOM PENH")
                                .merchantId(merchantId)
                                .acquiringBank(acquiringBank)
                                .billNumber("ORDER-" + order.orderId())
                                .expirationTimestamp(15)
                                .purposeOfTransaction(
                                        "Order " + order.orderId()
                                )
                                .build()
                )
        );
        String qr = generated.path("data").path("qr").asText("");
        String md5 = generated.path("data").path("md5").asText("");

        if (qr.isBlank() || !md5.matches("[a-fA-F0-9]{32}")) {
            throw new IllegalStateException("QR generation failed");
        }

        var created = transactionService.createTransaction(
                PaymentTransactionRequest.builder()
                        .orderId(order.orderId())
                        .customerId(order.userId())
                        .amount(order.amount())
                        .currency(order.currency())
                        .paymentMethod(PaymentMethod.BAKONG)
                        .remarks(
                                "Dynamic KHQR; authoritative verification required"
                        )
                        .build()
        );
        var record = transactions.findById(created.getId()).orElseThrow();
        record.setQrPayload(qr);
        record.setQrMd5(md5.toLowerCase(Locale.ROOT));
        record.setRecipientAccount(recipient);
        record.setExpiresAt(Instant.now().plusSeconds(900));
        record.setNextVerificationAt(Instant.now());
    }

    @Scheduled(fixedDelayString = "${automation.verification-delay-ms:15000}")
    public void reconcile() {
        for (Long id : transactions.dueVerification(
                Instant.now(),
                Instant.now().minusSeconds(2592000),
                PageRequest.of(0, 50)
        )) {
            try {
                verify(id);
            } catch (Exception ex) {
                tx.executeWithoutResult(t ->
                        transactions.lockById(id).ifPresent(record -> {
                            record.setVerificationAttempts(
                                    record.getVerificationAttempts() + 1
                            );
                            record.setNextVerificationAt(
                                    Instant.now().plusSeconds(60)
                            );

                            if (record.getVerificationAttempts() >= 10) {
                                events.review(
                                        record.getOrder(),
                                        "PROVIDER_VERIFICATION_UNAVAILABLE",
                                        "Verification failed repeatedly. "
                                                + "Payment remains pending; "
                                                + "do not assume success or "
                                                + "release stock."
                                );
                            }
                        })
                );
            }
        }
    }

    public void verify(long id) {
        var record = transactions.findById(id).orElseThrow();

        if (record.getQrMd5() == null
                || record.getStatus() != TransactionStatus.PENDING
                && record.getStatus() != TransactionStatus.FAILED) {
            return;
        }

        var response = bakong.checkTransactionByMD5(
                new CheckTransactionRequest(record.getQrMd5())
        );
        String hash = BakongVerificationPolicy.verifiedHash(
                response,
                record.getAmount(),
                record.getCurrency(),
                record.getRecipientAccount(),
                json
        );

        tx.executeWithoutResult(t -> {
            var locked = transactions.lockById(id).orElseThrow();

            if (locked.getStatus() != TransactionStatus.PENDING
                    && locked.getStatus() != TransactionStatus.FAILED) {
                return;
            }

            locked.setVerificationAttempts(
                    locked.getVerificationAttempts() + 1
            );
            locked.setNextVerificationAt(Instant.now().plusSeconds(30));

            if (hash != null) {
                locked.setProviderHash(hash);
                locked.setVerifiedAt(Instant.now());
                audit.record(
                        new com.example.eshop.common.audit.AuditEvent(
                                com.example.eshop.common.audit.AuditAction.UPDATE,
                                "PAYMENT",
                                Long.toString(id),
                                null,
                                Map.of(
                                        "status", "SUCCESS",
                                        "verification", "BAKONG_PROVIDER",
                                        "orderId", locked.getOrder()
                                ),
                                com.example.eshop.common.audit.AuditResult.SUCCESS,
                                null
                        )
                );
                setStatus(
                        locked,
                        TransactionStatus.SUCCESS,
                        "BAKONG_PROVIDER_VERIFIED"
                );
                var payment = payments.findByOrderId(locked.getOrder())
                        .orElseGet(Payment::new);
                payment.setOrderId(locked.getOrder());
                payment.setUserId(locked.getCustomer());
                payment.setAmount(locked.getAmount());
                payment.setCurrency(locked.getCurrency());
                payment.setPaymentMethod("BAKONG");
                payment.setPaymentProvider("BAKONG");
                payment.setTransactionId(hash);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setStatus("COMPLETED");
                payments.saveAndFlush(payment);
                events.emit(
                        CommerceEvent.PAYMENTS,
                        AutomationEvents.event(
                                CommerceEvent.Type.PAYMENT_VERIFIED,
                                locked.getOrder(),
                                locked.getCustomer(),
                                locked.getAmount(),
                                "BAKONG",
                                id
                        )
                );
                events.review(
                        locked.getOrder(),
                        "POST_PAYMENT_REVIEW",
                        "Verified provider evidence recorded. Review order, "
                                + "reservation and delivery status using "
                                + "approved read-only AI tools."
                );
            } else if (locked.getStatus() == TransactionStatus.PENDING
                    && BakongVerificationPolicy.notFound(response)
                    && Instant.now().isAfter(
                    locked.getExpiresAt().plusSeconds(60)
            )) {
                setStatus(
                        locked,
                        TransactionStatus.FAILED,
                        "QR_EXPIRED_PROVIDER_NOT_FOUND"
                );
                locked.setNextVerificationAt(
                        Instant.now().plusSeconds(3600)
                );
                events.emit(
                        CommerceEvent.PAYMENTS,
                        AutomationEvents.event(
                                CommerceEvent.Type.PAYMENT_EXPIRED,
                                locked.getOrder(),
                                locked.getCustomer(),
                                locked.getAmount(),
                                "BAKONG",
                                id
                        )
                );
            } else if (!BakongVerificationPolicy.notFound(response)) {
                throw new IllegalStateException(
                        "Provider outcome unavailable"
                );
            } else if (locked.getStatus() == TransactionStatus.FAILED) {
                locked.setNextVerificationAt(
                        Instant.now().plusSeconds(3600)
                );
            }
        });
    }

    private void setStatus(
            PaymentTransaction record,
            TransactionStatus status,
            String reason
    ) {
        history.save(
                PaymentTransactionStatusHistory.builder()
                        .transaction(record.getId())
                        .oldStatus(record.getStatus())
                        .newStatus(status)
                        .changedAt(LocalDateTime.now())
                        .changedBy("payment-provider-verifier")
                        .reason(reason)
                        .build()
        );
        record.setStatus(status);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> customerQr(long orderId) {
        var order = orders.get(orderId);

        if (order == null
                || order.userId() != CurrentActor.userId()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Order unavailable"
            );
        }

        var record = transactions.findByOrder(orderId)
                .stream()
                .filter(t -> t.getQrMd5() != null)
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException(
                        "QR is not ready. Check automation status or retry shortly."
                ));
        return Map.of(
                "paymentId", record.getId(),
                "orderId", orderId,
                "amount", record.getAmount(),
                "currency", record.getCurrency(),
                "status", record.getStatus(),
                "qr", record.getQrPayload(),
                "expiresAt", record.getExpiresAt(),
                "provider", "BAKONG_KHQR"
        );
    }

    @Transactional(readOnly = true)
    public VerifiedPaymentSnapshot snapshot(long id) {
        var p = transactions.findById(id).orElseThrow();
        return new VerifiedPaymentSnapshot(
                id,
                p.getOrder(),
                p.getCustomer(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus().name(),
                p.getProviderHash(),
                p.getVerifiedAt(),
                p.getExpiresAt()
        );
    }
}