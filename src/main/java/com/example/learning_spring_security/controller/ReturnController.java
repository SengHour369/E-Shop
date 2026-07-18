package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.ReturnService;
import com.example.learning_spring_security.dto.Request.*;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping("/user")
    public ResponseEntity<ResponseErrorTemplate> createReturn(@Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.ok(returnService.createReturn(request));
    }

    @GetMapping("/summary")
    public ResponseEntity<ResponseErrorTemplate> getReturnSummary() {
        return ResponseEntity.ok(returnService.getReturnSummary());
    }


    @PostMapping("/returns")
    public ResponseEntity<ResponseErrorTemplate> getReturns(@Valid @RequestBody  GetReturnRequest request) {
        return ResponseEntity.ok(returnService.getReturns(request));
    }
    @GetMapping("/{returnId}")
    public ResponseEntity<ResponseErrorTemplate> getReturnDetail(@PathVariable String returnId) {
        return ResponseEntity.ok(returnService.getReturnDetail(returnId));
    }

    @GetMapping("/{returnId}/history")
    public ResponseEntity<ResponseErrorTemplate> getReturnHistory(@PathVariable String returnId) {
        return ResponseEntity.ok(returnService.getReturnHistory(returnId));
    }

    @PostMapping("/{returnId}/approve")
    public ResponseEntity<ResponseErrorTemplate> approveReturn(
            @PathVariable String returnId,
            @RequestBody(required = false) ApproveReturnRequest request) {
        return ResponseEntity.ok(returnService.approveReturn(returnId, request));
    }

    @PostMapping("/{returnId}/reject")
    public ResponseEntity<ResponseErrorTemplate> rejectReturn(
            @PathVariable String returnId,
            @RequestBody(required = false) RejectReturnRequest request) {
        return ResponseEntity.ok(returnService.rejectReturn(returnId, request));
    }

    @PostMapping("/{returnId}/receive")
    public ResponseEntity<ResponseErrorTemplate> receiveReturn(
            @PathVariable String returnId,
            @RequestBody(required = false) ReceiveReturnRequest request) {
        return ResponseEntity.ok(returnService.receiveReturn(returnId, request));
    }

    @PostMapping("/{returnId}/inspect/start")
    public ResponseEntity<ResponseErrorTemplate> startInspection(@PathVariable String returnId) {
        return ResponseEntity.ok(returnService.startInspection(returnId));
    }

    @PostMapping("/{returnId}/inspect/complete")
    public ResponseEntity<ResponseErrorTemplate> completeInspection(
            @PathVariable String returnId,
            @RequestBody(required = false) CompleteInspectionRequest request) {
        return ResponseEntity.ok(returnService.completeInspection(returnId, request));
    }
}