package com.example.learning_spring_security.dto.Request;

import com.example.learning_spring_security.Enumeration.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class PaymentTransactionStatusUpdateRequest {

    @NotNull(message = "New status is required")
    @JsonProperty("new_status")
    private TransactionStatus newStatus;

    @JsonProperty("changed_by")
    private String changedBy;

    private String reason;
}