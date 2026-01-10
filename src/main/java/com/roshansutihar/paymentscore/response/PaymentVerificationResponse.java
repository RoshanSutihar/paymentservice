package com.roshansutihar.paymentscore.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentVerificationResponse {
    private boolean valid;
    private String sessionId;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private String status;
    private LocalDateTime expiryTime;
    private String message;
}
