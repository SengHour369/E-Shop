package com.example.eshop.payment.bakong;

/** Individual KHQR has no merchant ID/acquiring-bank requirement. Merchant mode retains its checks. */
public enum KhqrType {
    MERCHANT, INDIVIDUAL;
    public boolean configured(String account, String name, String merchant, String bank) {
        if (account == null || account.isBlank()) return false;
        return this == INDIVIDUAL || has(name) && has(merchant) && has(bank);
    }
    private static boolean has(String value) { return value != null && !value.isBlank(); }
}
