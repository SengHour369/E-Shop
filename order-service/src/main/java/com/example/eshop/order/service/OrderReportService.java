package com.example.eshop.order.service;

import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.dto.response.MonthlyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderReportService {
    private final OrderRepository repository;

    @Transactional(readOnly = true)
    public List<com.example.eshop.order.dto.response.DailyReportResponse> weekly(LocalDate weekOf) {
        var start = weekOf.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        return repository.dailyReport(start, start.plusWeeks(1));
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportResponse> monthly(int year) {
        var start = LocalDate.of(year, 1, 1).atStartOfDay();
        return repository.monthlyReport(start, start.plusYears(1));
    }
}
