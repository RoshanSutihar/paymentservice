package com.roshansutihar.paymentscore.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MerchantTransactionDTO {
    private String sessionId;
    private String transactionRef;
    @Getter
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    @Getter
    private BigDecimal commissionAmount;
    @Getter
    private BigDecimal netAmount;
    private LocalDateTime settlementDate;

}
