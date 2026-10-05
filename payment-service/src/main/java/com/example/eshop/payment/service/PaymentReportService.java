package com.example.eshop.payment.service;

import com.example.eshop.payment.repository.PaymentRepository;
import com.example.eshop.payment.dto.response.MonthlyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReportService {
    private final PaymentRepository repository;

    @Transactional(readOnly = true)
    public List<MonthlyReportResponse> monthly(int year) {
        var start = LocalDate.of(year, 1, 1).atStartOfDay();
        return repository.monthlyReport(start, start.plusYears(1));
    }
}
