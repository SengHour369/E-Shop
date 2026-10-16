package com.example.eshop.payment.automation;
import com.example.eshop.payment.bakong.service.impl.BakongServiceImpl;
import com.example.eshop.payment.bakong.service.BakongTokenService;
import com.example.eshop.payment.bakong.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class KhqrRenderingTest {
    @Test void individualQrUsesConfiguredAccountAndActualOrderWithoutMerchantFields(){
        var rest=mock(RestClient.class);var tokens=mock(BakongTokenService.class);var mapper=new ObjectMapper();
        var service=new BakongServiceImpl(rest,mapper,tokens);
        ReflectionTestUtils.setField(service,"bakongAccountId","senghour_soeurng@bkrt");
        ReflectionTestUtils.setField(service,"khqrType",com.example.eshop.payment.bakong.KhqrType.INDIVIDUAL);
        var response=mapper.valueToTree(service.generateQR(BakongRequest.builder().currency("USD").amount(10.0)
            .billNumber("ORDER-12").expirationTimestamp(15).purposeOfTransaction("Order 12").build()));
        String qr=response.path("data").path("qr").asText();
        assertThat(qr).startsWith("000201").contains("senghour_soeurng@bkrt","ORDER-12");
        assertThat(qr).doesNotContain("DEFAULT", "012345678");
        assertThat(response.path("data").path("md5").asText()).matches("[a-fA-F0-9]{32}");
        var decoded=mapper.valueToTree(kh.gov.nbc.bakong_khqr.BakongKHQR.decode(qr));
        assertThat(decoded.path("data").path("bakongAccountID").asText()).isEqualTo("senghour_soeurng@bkrt");
        assertThat(decoded.path("data").path("merchantId").asText("")).isEmpty();
        assertThat(decoded.path("data").path("acquiringBank").asText("")).isEmpty();
        assertThat(decoded.path("data").path("transactionAmount").asText()).isEqualTo("10");
        assertThat(kh.gov.nbc.bakong_khqr.BakongKHQR.verify(qr).getData().isValid()).isTrue();
        assertThat(service.getQRImage(new GetQRImageRequest(qr,""))).startsWith((byte)137,(byte)80,(byte)78,(byte)71);
        verifyNoInteractions(rest,tokens);
    }

    @Test void realSdkProducesOrderBoundQrAndPngWithoutProviderNetwork(){
        var rest=mock(RestClient.class);var tokens=mock(BakongTokenService.class);var mapper=new ObjectMapper();
        var service=new BakongServiceImpl(rest,mapper,tokens);
        ReflectionTestUtils.setField(service,"bakongAccountId","merchant@bank");
        var response=mapper.valueToTree(service.generateQR(BakongRequest.builder().currency("USD").amount(10.0)
          .merchantName("TEST SHOP").merchantCity("PHNOM PENH").merchantId("123456").acquiringBank("TEST BANK")
          .billNumber("ORDER-12").expirationTimestamp(15).purposeOfTransaction("Order 12").build()));
        String qr=response.path("data").path("qr").asText();
        assertThat(qr).startsWith("000201").contains("ORDER-12");assertThat(response.path("data").path("md5").asText()).matches("[a-fA-F0-9]{32}");
        byte[] png=service.getQRImage(new GetQRImageRequest(qr,""));assertThat(png).startsWith((byte)137,(byte)80,(byte)78,(byte)71);
        verifyNoInteractions(rest,tokens);
    }
}
