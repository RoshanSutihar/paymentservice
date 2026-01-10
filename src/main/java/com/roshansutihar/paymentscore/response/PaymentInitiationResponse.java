package com.roshansutihar.paymentscore.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentInitiationResponse {
    private boolean success;
    private String sessionId;
    private String qrData;
    private LocalDateTime expiryTime;
    private String message;
}
