package com.roshansutihar.paymentscore.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentStatusResponse {
    private String sessionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String merchantId;
    private LocalDateTime createdAt;
    private LocalDateTime expiryTime;
    private String transactionRef;
}
