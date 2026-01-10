package com.roshansutihar.paymentscore.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletionResponse {
    private boolean success;
    private String transactionId;
    private String sessionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String merchantId;
    private LocalDateTime completedAt;
    private String message;
}
