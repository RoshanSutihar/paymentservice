package com.roshansutihar.paymentscore.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCancellationRequest {
    private String sessionId;
    private String reason;
}
