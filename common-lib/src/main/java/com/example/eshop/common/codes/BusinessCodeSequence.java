package com.example.eshop.common.codes;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Locale;

/** Atomic per-database document numbering. Never used as an authorization credential. */
public final class BusinessCodeSequence {
    public enum Kind {
        ORDER("anOrder_"), PAYMENT("anPayment_");
        private final String prefix;
        Kind(String prefix) { this.prefix = prefix; }
    }
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public BusinessCodeSequence(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
    }
    public String next(Kind kind) {
        return transactions.execute(status -> {
            Long previous = jdbc.queryForObject(
                    "SELECT last_value FROM business_code_counters WHERE code_type = ? FOR UPDATE",
                    Long.class, kind.name());
            if (previous == null || previous < 0) throw new IllegalStateException("Invalid business code counter");
            long value = Math.addExact(previous, 1L);
            int updated = jdbc.update("UPDATE business_code_counters SET last_value = ? WHERE code_type = ?", value, kind.name());
            if (updated != 1) throw new IllegalStateException("Business code counter is missing; apply its database migration");
            return kind.prefix + String.format(Locale.ROOT, "%03d", value);
        });
    }
}
