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
public class MerchantProfileResponse {
    private String merchantId;
    private String storeName;
    private String callbackUrl;
    private String status;
    private String commissionType;
    private BigDecimal commissionValue;
    private BigDecimal minCommission;
    private BigDecimal maxCommission;
    private LocalDateTime createdAt;
    private String bankAccountNumber;
    private String bankRoutingNumber;
}