package com.example.learning_spring_security.controller;


import com.example.learning_spring_security.Service.ServiceStructure.RefundService;
import com.example.learning_spring_security.dto.Request.CancelRefundRequest;
import com.example.learning_spring_security.dto.Request.GetRefundListRequest;
import com.example.learning_spring_security.dto.Request.ProcessRefundRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/refunds")
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
}