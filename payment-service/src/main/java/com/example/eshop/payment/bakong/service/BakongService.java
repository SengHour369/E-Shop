package com.example.eshop.payment.bakong.service;

import com.example.eshop.payment.bakong.dto.BakongRequest;
import com.example.eshop.payment.bakong.dto.BakongResponse;
import com.example.eshop.payment.bakong.dto.CheckTransactionRequest;
import com.example.eshop.payment.bakong.dto.GetQRImageRequest;
import jakarta.validation.Valid;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRDeepLinkData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

public interface BakongService {

    KHQRResponse<KHQRData> generateQR(BakongRequest request);
    byte[] getQRImage(@Valid GetQRImageRequest qr);
    BakongResponse checkTransactionByMD5(CheckTransactionRequest request);
    KHQRResponse<KHQRDeepLinkData> generateDeepLink(String qr);
}
