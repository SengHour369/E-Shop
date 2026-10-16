package com.example.eshop.payment;

import com.example.eshop.payment.bakong.BakongQrRenderer;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayInputStream;
import java.awt.Color;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;

class BakongQrBrandingTest {
    @ParameterizedTest @ValueSource(doubles={0.01,10.0,12345.67})
    void logoKeepsPaymentQrReadableAndLeavesPayloadUnchanged(double amount)throws Exception {
        var individual=new IndividualInfo();individual.setBakongAccountId("senghour_soeurng@bkrt");
        individual.setMerchantName("senghour_soeurng");individual.setMerchantCity("KH");
        individual.setCurrency(KHQRCurrency.USD);individual.setAmount(amount);individual.setBillNumber("TEST-QR-1792175400752");
        individual.setPurposeOfTransaction("QR generation test");individual.setExpirationTimestamp(System.currentTimeMillis()+900_000);
        check(BakongKHQR.generateIndividual(individual).getData().getQr());
        var merchant=new MerchantInfo();merchant.setBakongAccountId("merchant@bank");merchant.setMerchantId("123456");
        merchant.setAcquiringBank("TEST BANK");merchant.setMerchantName("TEST SHOP");merchant.setMerchantCity("PHNOM PENH");
        merchant.setCurrency(KHQRCurrency.USD);merchant.setAmount(amount);merchant.setBillNumber("ORDER-123");
        merchant.setExpirationTimestamp(System.currentTimeMillis()+900_000);
        check(BakongKHQR.generateMerchant(merchant).getData().getQr());
    }
    private void check(String payload)throws Exception {
        var image=ImageIO.read(new ByteArrayInputStream(BakongQrRenderer.render(payload)));
        assertThat(image.getWidth()).isEqualTo(300);assertThat(image.getHeight()).isEqualTo(300);
        boolean red=false;
        for(int x=132;x<168;x++)for(int y=132;y<168;y++){
            var pixel=new Color(image.getRGB(x,y));if(pixel.getRed()>200&&pixel.getGreen()<70&&pixel.getBlue()<90)red=true;
        }
        assertThat(red).as("supplied red Bakong logo is present").isTrue();
        assertThat(new Color(image.getRGB(0,0))).isEqualTo(Color.WHITE);
        var bitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(payload);
        // Customer chat displays the image at 240px; validate that presentation size too.
        var small=new java.awt.image.BufferedImage(240,240,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=small.createGraphics();
        try {graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);graphics.drawImage(image,0,0,240,240,null);} finally {graphics.dispose();}
        var smallBitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(small)));
        assertThat(new MultiFormatReader().decode(smallBitmap).getText()).isEqualTo(payload);
        assertThat(BakongKHQR.verify(payload).getData().isValid()).isTrue();
    }
}
