package com.example.learning_spring_security.Bakong.service.impl.service.impl;

import com.example.learning_spring_security.Bakong.service.impl.dto.BakongRequest;
import com.example.learning_spring_security.Bakong.service.impl.dto.BakongResponse;
import com.example.learning_spring_security.Bakong.service.impl.dto.CheckTransactionRequest;
import com.example.learning_spring_security.Bakong.service.impl.dto.GetQRImageRequest;
import com.example.learning_spring_security.Bakong.service.impl.service.BakongService;
import com.example.learning_spring_security.Bakong.service.impl.service.BakongTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.validation.Valid;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.MerchantInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BakongServiceImpl implements BakongService {

    @Value("${bakong.account-id}")
    private String bakongAccountId;
    @Value("${bakong.base-url}")
    private String baseUrl;

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final BakongTokenService bakongTokenService;

    @Override
    public KHQRResponse<KHQRData> generateQR(BakongRequest bakongRequest) {

        MerchantInfo merchantInfo = new MerchantInfo();

        // Set expiration timestamp to current time + provided expiration or default to 15 minutes
        // Bakong API expects expiration timestamp in milliseconds, so we convert minutes to milliseconds
        // You just need to provide expiration in minutes, and we will handle the conversion and defaulting logic here
        merchantInfo.setExpirationTimestamp(
                System.currentTimeMillis() + bakongRequest.expirationTimestamp() * 60 * 1000
        );

        merchantInfo.setBakongAccountId(bakongAccountId);
        merchantInfo.setMerchantId(bakongRequest.merchantId());
        merchantInfo.setAcquiringBank(bakongRequest.acquiringBank());
        merchantInfo.setCurrency(KHQRCurrency.valueOf(bakongRequest.currency()));
        merchantInfo.setAmount(bakongRequest.amount());
        merchantInfo.setMerchantName(bakongRequest.merchantName());
        merchantInfo.setMerchantCity(bakongRequest.merchantCity());
        merchantInfo.setBillNumber(bakongRequest.billNumber());
        merchantInfo.setMobileNumber(bakongRequest.mobileNumber());
        merchantInfo.setStoreLabel(bakongRequest.storeLabel());
        merchantInfo.setUpiAccountInformation(bakongRequest.upiAccountInformation());
        merchantInfo.setMerchantAlternateLanguagePreference(bakongRequest.merchantAlternateLanguagePreference());
        merchantInfo.setMerchantNameAlternateLanguage(bakongRequest.merchantNameAlternateLanguage());
        merchantInfo.setMerchantCityAlternateLanguage(bakongRequest.merchantCityAlternateLanguage());
        merchantInfo.setPurposeOfTransaction(bakongRequest.purposeOfTransaction());
        merchantInfo.setTerminalLabel(bakongRequest.terminalLabel());
        return BakongKHQR.generateMerchant(merchantInfo);
    }


    @Override
    public byte[] getQRImage(@Valid GetQRImageRequest qr) {
        try {
            // Validate input
            if (qr == null || qr.qr() == null || qr.qr().isBlank()) {
                return "Invalid QR data".getBytes(StandardCharsets.UTF_8);
            }

            String qrCodeText = qr.qr();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();

        } catch (WriterException e) {
            log.error("Error encoding QR data: {}", e.getMessage());
            return "Error encoding QR data".getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return ("Unexpected error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public BakongResponse checkTransactionByMD5(CheckTransactionRequest request) {
        try {
            String bearerToken = bakongTokenService.getToken();
            String url = baseUrl.replaceAll("/+$", "") + "/v1/check_transaction_by_md5";

            log.info("Sending md5 to Bakong: {}", request.md5());

            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .body(Map.of("md5", request.md5()))
                    .retrieve()
                    .body(String.class);

            log.info("Bakong response: {}", responseBody);

            return mapper.readValue(responseBody, BakongResponse.class);

        } catch (Exception e) {
            log.error("Error checking transaction: {}", e.getMessage(), e);
            BakongResponse response = new BakongResponse();
            response.setResponseCode(-1);
            response.setResponseMessage("Failed to check transaction: " + e.getMessage());
            return response;
        }
    }

    /**
     * Build KHQR string according to Bakong KHQR standard
     */
    private String buildKHQRString(BakongRequest request) {
        // Simple KHQR string format
        return String.format("00020101021229370016A0000000727302150%s0215%s5303%s%s",
                bakongAccountId,
                request.merchantId(),
                request.currency(),
                request.amount() != null ? String.format("%010d", request.amount().longValue()) : "0");
    }
}
