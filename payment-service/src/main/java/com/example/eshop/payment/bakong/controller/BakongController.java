package com.example.eshop.payment.bakong.controller;

import com.example.eshop.payment.bakong.dto.BakongResponse;
import com.example.eshop.payment.bakong.dto.CheckTransactionRequest;
import com.example.eshop.payment.bakong.dto.GetQRImageRequest;
import com.example.eshop.payment.bakong.service.BakongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/bakong")
@RequiredArgsConstructor
public class BakongController {

    private final BakongService service;

    @PostMapping("/get-qr-image")
    public ResponseEntity<byte[]> getQRImage(@Valid @RequestBody GetQRImageRequest request) {
        try {
            byte[] imageBytes = service.getQRImage(request);

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode.png\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage().getBytes());
        }
    }

    @PostMapping("/check-transaction")
    public ResponseEntity<BakongResponse> checkTransaction(
            @Valid @RequestBody CheckTransactionRequest request) {

        BakongResponse response = service.checkTransactionByMD5(request);
        return ResponseEntity.ok(response);
    }

}
