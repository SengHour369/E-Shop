package com.example.eshop.common.storefront;

/** Only effective public feature flags; contains no contact, account or payment secrets. */
public record StorefrontAccess(
        boolean guestBrowsing,
        boolean guestReviews,
        boolean guestCart,
        boolean guestCheckout,
        boolean guestPayment,
        boolean requireDeliveryAddress,
        boolean requireGuestContactVerification,
        boolean guestOrderTracking
) {

    public static StorefrontAccess defaults() {
        return new StorefrontAccess(
                true,
                false,
                false,
                false,
                false,
                true,
                false,
                false
        );
    }
}