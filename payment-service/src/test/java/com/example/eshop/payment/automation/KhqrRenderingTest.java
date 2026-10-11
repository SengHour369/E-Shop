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
