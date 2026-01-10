package com.roshansutihar.paymentscore.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentInitiationRequest {
    private String terminalId;
    private BigDecimal amount;
    private String transactionRef;
    private String callbackUrl;
}
