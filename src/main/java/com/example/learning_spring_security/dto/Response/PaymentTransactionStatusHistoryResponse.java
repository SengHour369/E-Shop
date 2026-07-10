package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class PaymentTransactionStatusHistoryResponse {

    private Long id;

    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("old_status")
    private String oldStatus;

    @JsonProperty("new_status")
    private String newStatus;

    @JsonProperty("changed_at")
    private LocalDateTime changedAt;

    @JsonProperty("changed_by")
    private String changedBy;

    private String reason;
}