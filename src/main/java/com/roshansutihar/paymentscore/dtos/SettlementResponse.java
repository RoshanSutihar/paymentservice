package com.roshansutihar.paymentscore.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    private String settlementId;
    private String merchantId;
    private String bankAccountNumber;
    private String bankRoutingNumber;
    private LocalDateTime generatedAt;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
    private List<SettlementTransaction> transactions;
    private Integer transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal totalFees;
    private BigDecimal totalNetAmount;
    private String status;
}