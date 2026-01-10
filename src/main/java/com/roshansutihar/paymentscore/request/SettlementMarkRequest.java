package com.roshansutihar.paymentscore.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementMarkRequest {
    @NotBlank(message = "Settlement ID is required")
    private String settlementId;

    @NotEmpty(message = "Transaction references cannot be empty")
    private List<String> transactionRefs;

    @NotNull(message = "Total amount is required")
    private Double totalAmount;

    @NotNull(message = "Total net amount is required")
    private Double totalNetAmount;

    @NotNull(message = "Total fees is required")
    private Double totalFees;
}
