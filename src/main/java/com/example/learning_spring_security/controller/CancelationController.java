package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.CancelationQueryService;
import com.example.learning_spring_security.dto.Request.GetCancelationListRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/cancelations")
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