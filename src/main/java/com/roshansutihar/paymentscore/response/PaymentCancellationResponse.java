package com.roshansutihar.paymentscore.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCancellationResponse {
    private boolean success;
    private String sessionId;
    private String status;
    private String message;
}