package com.roshansutihar.paymentscore.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MerchantTransactionDetailDTO {
    private String sessionId;
    private String transactionRef;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private CommissionDTO commission;
    private List<EventDTO> events;
    private RailTransferDTO railTransfer;
}
