package com.roshansutihar.paymentscore.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantUpdateRequest {
    private String storeName;
    private String callbackUrl;
    private String status;
    private String commissionType;
    private BigDecimal commissionValue;
    private BigDecimal minCommission;
    private BigDecimal maxCommission;
}
