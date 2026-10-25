package com.example.eshop.order.controller;
import com.example.eshop.order.service.SalesReportDetailsService;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class SalesReportDetailsController {
 private final SalesReportDetailsService reports;
 @GetMapping("/api/v1/orders/reports/sales-details")
 @PreAuthorize("hasAnyAuthority('ADMIN','MANAGER')")
 public Object details(@RequestParam int year,@RequestHeader("Authorization") String bearer){
  if(year<2000||year>2100)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Choose a year between 2000 and 2100.");
  return ResponseErrorTemplate.success("Sales report details",reports.yearly(year,bearer));
 }
}
