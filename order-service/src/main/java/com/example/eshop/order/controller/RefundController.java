package com.example.eshop.order.controller;

import com.example.eshop.order.service.RefundService;
import com.example.eshop.order.dto.request.CancelRefundRequest;
import com.example.eshop.order.dto.request.GetRefundListRequest;
import com.example.eshop.order.dto.request.ProcessRefundRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @GetMapping("/summary")
    public ResponseEntity<ResponseErrorTemplate> getRefundSummary() {
        return ResponseEntity.ok(refundService.getRefundSummary());
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseErrorTemplate> getRefundList(@RequestBody GetRefundListRequest request) {
        return ResponseEntity.ok(refundService.getRefundList(request));
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<ResponseErrorTemplate> getRefundDetail(@PathVariable String refundId) {
        return ResponseEntity.ok(refundService.getRefundDetail(refundId));
    }

    @PostMapping("/{refundId}/process")
    public ResponseEntity<ResponseErrorTemplate> processRefund(
            @PathVariable String refundId,
            @RequestBody(required = false) ProcessRefundRequest request) {
        return ResponseEntity.ok(refundService.processRefund(refundId, request));
    }

    @PostMapping("/{refundId}/cancel")
    public ResponseEntity<ResponseErrorTemplate> cancelRefund(
            @PathVariable String refundId,
            @RequestBody(required = false) CancelRefundRequest request) {
        return ResponseEntity.ok(refundService.cancelRefund(refundId, request));
    }

    @GetMapping("/{refundId}/history")
    public ResponseEntity<ResponseErrorTemplate> getRefundHistory(@PathVariable String refundId) {
        return ResponseEntity.ok(refundService.getRefundHistory(refundId));
    }

    @GetMapping("/{refundId}/similar-products")
    public ResponseEntity<ResponseErrorTemplate> getSimilarProducts(
            @PathVariable String refundId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(refundService.getSimilarProducts(refundId, page, size));
    }
}
