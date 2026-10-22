package com.example.eshop.order.guest;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.Duration;

/** Redacts guest contact data without deleting accounting or inventory history. */
@Service @RequiredArgsConstructor
public class GuestRetentionService {
    private final JdbcTemplate jdbc;
    @Scheduled(cron="0 20 3 * * *") @Transactional
    public void redact() {
        Instant now=Instant.now();
        jdbc.update("DELETE FROM guest_order_access WHERE expires_at < ?",java.sql.Timestamp.from(now));
        jdbc.update("UPDATE guest_sessions SET verified_email=NULL,contact_verified_at=NULL WHERE expires_at < ?",
                java.sql.Timestamp.from(now.minus(Duration.ofDays(30))));
        // Keep the immutable address while the delivery/accounting workflow is active; redact terminal orders after 90 days.
        jdbc.update("UPDATE order_details SET shipping_address_snapshot='{}', verified_contact_email=NULL WHERE guest_session_id IS NOT NULL "
                +"AND status IN ('DELIVERED','CANCELLED','FAILED','REFUNDED') AND order_date < ? AND shipping_address_snapshot <> '{}'",
                java.sql.Timestamp.from(now.minus(Duration.ofDays(90))));
        jdbc.update("DELETE FROM cart_items WHERE cart_id IN (SELECT c.id FROM carts c JOIN guest_sessions g ON c.guest_session_id=g.id WHERE g.expires_at < ? AND c.checkout_order_id IS NULL)",
                java.sql.Timestamp.from(now.minus(Duration.ofDays(30))));
        jdbc.update("DELETE FROM carts c USING guest_sessions g WHERE c.guest_session_id=g.id AND g.expires_at < ? AND c.checkout_order_id IS NULL",
                java.sql.Timestamp.from(now.minus(Duration.ofDays(30))));
        jdbc.update("DELETE FROM guest_sessions g WHERE g.expires_at < ? AND NOT EXISTS(SELECT 1 FROM order_details o WHERE o.guest_session_id=g.id) "
                +"AND NOT EXISTS(SELECT 1 FROM carts c WHERE c.guest_session_id=g.id)",java.sql.Timestamp.from(now.minus(Duration.ofDays(30))));
    }
}
