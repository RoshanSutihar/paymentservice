package com.roshansutihar.paymentscore.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TransactionSummaryResponse {
    private String merchantId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Integer totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal totalNetAmount;
    private BigDecimal totalCommission;
    private Map<String, Long> statusBreakdown;
}
