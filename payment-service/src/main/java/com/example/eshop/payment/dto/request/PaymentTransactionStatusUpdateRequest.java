package com.example.eshop.payment.dto.request;

import com.example.eshop.payment.enumeration.TransactionStatus;
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
