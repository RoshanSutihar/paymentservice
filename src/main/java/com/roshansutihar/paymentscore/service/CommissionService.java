package com.roshansutihar.paymentscore.service;

import com.roshansutihar.paymentscore.entity.MerchantAccount;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionService {

    public CommissionCalculation calculateCommission(MerchantAccount merchant, BigDecimal transactionAmount) {
        log.info("Calculating commission for merchant: {}, amount: {}, commissionType: {}, commissionValue: {}, min: {}, max: {}",
                merchant.getMerchantId(), transactionAmount, merchant.getCommissionType(),
                merchant.getCommissionValue(), merchant.getMinCommission(), merchant.getMaxCommission());

        BigDecimal commissionAmount;

        if ("PERCENTAGE".equals(merchant.getCommissionType())) {
            if (merchant.getCommissionValue().compareTo(BigDecimal.ONE) <= 0) {
                // Already in decimal format (e.g., 0.02 for 2%)
                commissionAmount = transactionAmount.multiply(merchant.getCommissionValue());
            } else {
                // In percentage format (e.g., 2.0 for 2%), need to divide by 100
                commissionAmount = transactionAmount.multiply(merchant.getCommissionValue())
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            }
        } else {
            // Fixed commission
            commissionAmount = merchant.getCommissionValue();
        }

        // Apply minimum commission
        if (merchant.getMinCommission() != null && commissionAmount.compareTo(merchant.getMinCommission()) < 0) {
            log.debug("Applying minimum commission: {} instead of {}", merchant.getMinCommission(), commissionAmount);
            commissionAmount = merchant.getMinCommission();
        }

        // Apply maximum commission
        if (merchant.getMaxCommission() != null && commissionAmount.compareTo(merchant.getMaxCommission()) > 0) {
            log.debug("Applying maximum commission: {} instead of {}", merchant.getMaxCommission(), commissionAmount);
            commissionAmount = merchant.getMaxCommission();
        }

        // Ensure commission doesn't exceed transaction amount
        if (commissionAmount.compareTo(transactionAmount) > 0) {
            log.debug("Capping commission at transaction amount: {} instead of {}", transactionAmount, commissionAmount);
            commissionAmount = transactionAmount;
        }

        BigDecimal netAmount = transactionAmount.subtract(commissionAmount);

        CommissionCalculation result = CommissionCalculation.builder()
                .transactionAmount(transactionAmount)
                .commissionAmount(commissionAmount.setScale(2, RoundingMode.HALF_UP))
                .netAmount(netAmount.setScale(2, RoundingMode.HALF_UP))
                .commissionRate(merchant.getCommissionValue())
                .commissionType(merchant.getCommissionType())
                .build();

        log.info("Commission calculation result: transactionAmount={}, commissionAmount={}, netAmount={}",
                result.getTransactionAmount(), result.getCommissionAmount(), result.getNetAmount());

        return result;
    }

    @Data
    @Builder
    public static class CommissionCalculation {
        private BigDecimal transactionAmount;
        private BigDecimal commissionAmount;
        private BigDecimal netAmount;
        private BigDecimal commissionRate;
        private String commissionType;
    }
}