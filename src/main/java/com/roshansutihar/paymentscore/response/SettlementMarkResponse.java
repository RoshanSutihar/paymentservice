package com.roshansutihar.paymentscore.response;

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
public class SettlementMarkResponse {
    private Boolean success;
    private String settlementId;
    private String message;
    private LocalDateTime processedAt;
    private Integer transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal totalNetAmount;
    private BigDecimal totalFees;
}
