package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.ReturnService;
import com.example.learning_spring_security.dto.Request.ApproveReturnRequest;
import com.example.learning_spring_security.dto.Request.GetReturnListRequest;
import com.example.learning_spring_security.dto.Request.RejectReturnRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping("/summary")
    public ResponseEntity<ResponseErrorTemplate> getReturnSummary() {
        return ResponseEntity.ok(returnService.getReturnSummary());
    }

    @PostMapping("/list")
    public ResponseEntity<ResponseErrorTemplate> getReturnList(@RequestBody GetReturnListRequest request) {
        return ResponseEntity.ok(returnService.getReturnList(request));
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<ResponseErrorTemplate> getReturnDetail(@PathVariable String returnId) {
        return ResponseEntity.ok(returnService.getReturnDetail(returnId));
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
}