package com.example.eshop.order.controller;

import com.example.eshop.order.service.CancelationQueryService;
import com.example.eshop.order.dto.request.GetCancelationListRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cancelations")
@RequiredArgsConstructor
public class CancelationController {

    private final CancelationQueryService cancelationQueryService;

    @GetMapping("/summary")
    public ResponseEntity<ResponseErrorTemplate> getCancelationSummary() {
        return ResponseEntity.ok(cancelationQueryService.getCancelationSummary());
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseErrorTemplate> getCancelationList(@RequestBody GetCancelationListRequest request) {
        return ResponseEntity.ok(cancelationQueryService.getCancelationList(request));
    }

    @GetMapping("/{orderNo}")
    public ResponseEntity<ResponseErrorTemplate> getCancelationDetail(@PathVariable String orderNo) {
        return ResponseEntity.ok(cancelationQueryService.getCancelationDetail(orderNo));
    }
}
