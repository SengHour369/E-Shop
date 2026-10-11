package com.example.eshop.payment;
import com.example.eshop.payment.automation.BakongVerificationPolicy;
import com.example.eshop.payment.bakong.dto.BakongResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
class BakongVerificationPolicyTest {
    final ObjectMapper json=new ObjectMapper(); final String hash="a".repeat(64);
    BakongResponse response(String amount,String currency,String recipient) {
        return BakongResponse.builder().responseCode(0).data(Map.of("hash",hash,"amount",amount,"currency",currency,"toAccountId",recipient)).build();
    }
    @Test void verifiesExactAmountCurrencyAndRecipient() {
        assertThat(BakongVerificationPolicy.verifiedHash(response("10.00","USD","merchant@bank"),BigDecimal.TEN,"USD","merchant@bank",json)).isEqualTo(hash);
    }
    @Test void rejectsWrongAmount() { assertThatThrownBy(()->BakongVerificationPolicy.verifiedHash(response("9.99","USD","merchant@bank"),BigDecimal.TEN,"USD","merchant@bank",json)).isInstanceOf(IllegalStateException.class); }
    @Test void rejectsWrongCurrency() { assertThatThrownBy(()->BakongVerificationPolicy.verifiedHash(response("10","KHR","merchant@bank"),BigDecimal.TEN,"USD","merchant@bank",json)).isInstanceOf(IllegalStateException.class); }
    @Test void rejectsWrongMerchant() { assertThatThrownBy(()->BakongVerificationPolicy.verifiedHash(response("10","USD","attacker@bank"),BigDecimal.TEN,"USD","merchant@bank",json)).isInstanceOf(IllegalStateException.class); }
    @Test void aSuccessWordWithoutEvidenceCannotVerify() {
        var r=BakongResponse.builder().status("SUCCESS").build();
        assertThat(BakongVerificationPolicy.verifiedHash(r,BigDecimal.TEN,"USD","merchant@bank",json)).isNull();
    }
    @Test void missingEvidenceIsRejected() { assertThatThrownBy(()->BakongVerificationPolicy.verifiedHash(BakongResponse.builder().responseCode(0).build(),BigDecimal.TEN,"USD","merchant@bank",json)).isInstanceOf(IllegalStateException.class); }
    @Test void providerOutageIsNotPaymentFailure() {
        assertThat(BakongVerificationPolicy.notFound(BakongResponse.builder().responseCode(-1).build())).isFalse();
        assertThat(BakongVerificationPolicy.notFound(BakongResponse.builder().responseCode(1).errorCode(1).build())).isTrue();
    }
}
