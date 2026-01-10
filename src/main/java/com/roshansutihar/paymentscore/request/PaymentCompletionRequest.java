package com.roshansutihar.paymentscore.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCompletionRequest {
    private String sessionId;
    private String fromAccount;
    private String merchantId;
    private Double grossAmount;
    private String sourceRoutingNumber;
}
