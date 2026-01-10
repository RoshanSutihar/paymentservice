package com.roshansutihar.paymentscore.dtos;

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
public class SettlementTransaction {
    private String transactionRef;
    private String sessionId;
    private BigDecimal amount;
    private BigDecimal fees;
    private BigDecimal netAmount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime settlementDate;
}