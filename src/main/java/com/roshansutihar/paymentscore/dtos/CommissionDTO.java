package com.roshansutihar.paymentscore.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CommissionDTO {
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private BigDecimal commissionRate;
    private String commissionType;
}
