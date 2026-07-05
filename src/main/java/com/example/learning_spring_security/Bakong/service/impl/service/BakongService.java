package com.example.learning_spring_security.Bakong.service.impl.service;

import com.example.learning_spring_security.Bakong.service.impl.dto.*;
import jakarta.validation.Valid;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

public interface BakongService {

    KHQRResponse<KHQRData> generateQR(BakongRequest request);
    byte[] getQRImage(@Valid GetQRImageRequest qr);
    BakongResponse checkTransactionByMD5(CheckTransactionRequest request);
}
