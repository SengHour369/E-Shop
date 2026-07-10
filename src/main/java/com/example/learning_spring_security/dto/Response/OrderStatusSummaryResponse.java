package com.example.learning_spring_security.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusSummaryResponse {

    private Long totalOrders;

    private Long pending;

    private Long confirmed;

    private Long processing;

    private Long shipped;

    private Long delivered;

    private Long cancelled;

    private Long failed;

    private Long refunded;
}