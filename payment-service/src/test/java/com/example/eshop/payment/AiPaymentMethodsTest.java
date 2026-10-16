package com.example.eshop.payment;

import com.example.eshop.payment.controller.PaymentMethodsController;
import org.junit.jupiter.api.Test;
import com.example.eshop.payment.bakong.KhqrType;
import static org.assertj.core.api.Assertions.assertThat;

class AiPaymentMethodsTest {
    @Test void incompleteMerchantConfigurationDoesNotAdvertiseKhqr() {
        assertThat(new PaymentMethodsController(true,"account","name","","bank",KhqrType.MERCHANT).methods()).containsExactly("CASH_ON_DELIVERY");
        assertThat(new PaymentMethodsController(false,"account","name","merchant","bank",KhqrType.MERCHANT).methods()).containsExactly("CASH_ON_DELIVERY");
    }
    @Test void individualAccountDoesNotNeedMerchantFields() {
        assertThat(new PaymentMethodsController(true,"senghour_soeurng@bkrt","","","",KhqrType.INDIVIDUAL).methods()).contains("BAKONG","ABA","KHQR");
        assertThat(new PaymentMethodsController(true,"","","","",KhqrType.INDIVIDUAL).methods()).containsExactly("CASH_ON_DELIVERY");
    }
    @Test void configuredKhqrIsReportedWithoutExposingMerchantSecrets() {
        assertThat(new PaymentMethodsController(true,"account","name","merchant","bank",KhqrType.MERCHANT).methods()).contains("BAKONG","ABA","KHQR");
    }
}
