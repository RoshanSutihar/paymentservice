package com.roshansutihar.paymentscore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roshansutihar.paymentscore.entity.FraudCheck;
import com.roshansutihar.paymentscore.entity.PaymentIntent;
import com.roshansutihar.paymentscore.enums.RiskLevel;
import com.roshansutihar.paymentscore.repository.FraudCheckRepository;
import com.roshansutihar.paymentscore.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudCheckRepository fraudCheckRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ObjectMapper objectMapper;

    // Configurable rules
    @Value("${app.fraud.velocity.threshold:10}")
    private int velocityThreshold;

    @Value("${app.fraud.amount.threshold:5000}")
    private BigDecimal amountThreshold;

    @Value("${app.fraud.block.score:70}")
    private int blockRiskScore;

    @Transactional
    public FraudCheck performFraudCheck(PaymentIntent paymentIntent) {
        log.info("Performing fraud check for payment: {}", paymentIntent.getSessionId());

        FraudCheck fraudCheck = new FraudCheck();
        fraudCheck.setPaymentIntent(paymentIntent);

        int riskScore = 0;
        List<String> triggeredRules = new ArrayList<>();

        // Rule 1: Amount-based check
        riskScore += checkAmount(paymentIntent.getAmount(), triggeredRules);

        // Rule 2: Velocity check
        riskScore += checkTransactionVelocity(paymentIntent.getMerchantId(), triggeredRules);

        // Rule 3: Time-based check
        riskScore += checkTransactionTime(triggeredRules);

        // Rule 4: Duplicate transaction check
        riskScore += checkDuplicateTransactions(paymentIntent, triggeredRules);

        // Set risk level
        RiskLevel riskLevel = calculateRiskLevel(riskScore);

        fraudCheck.setRiskScore(riskScore);
        fraudCheck.setRiskLevel(riskLevel);
        fraudCheck.setRulesTriggered(triggeredRules);

        // Save to database
        FraudCheck savedCheck = fraudCheckRepository.save(fraudCheck);

        log.info("Fraud check completed for session: {}, Risk: {}, Score: {}, Rules: {}",
                paymentIntent.getSessionId(), riskLevel, riskScore, triggeredRules);

        return savedCheck;
    }

    private int checkAmount(BigDecimal amount, List<String> triggeredRules) {
        if (amount.compareTo(amountThreshold) > 0) {
            triggeredRules.add("HIGH_AMOUNT_TRANSACTION");
            return 30;
        } else if (amount.compareTo(amountThreshold.multiply(new BigDecimal("0.7"))) > 0) {
            triggeredRules.add("MEDIUM_AMOUNT_TRANSACTION");
            return 15;
        }
        return 0;
    }

    private int checkTransactionVelocity(String merchantId, List<String> triggeredRules) {

        Long recentTransactions = paymentIntentRepository.countRecentTransactionsByMerchant(
                merchantId, LocalDateTime.now().minusHours(1));

        if (recentTransactions > velocityThreshold) {
            triggeredRules.add("HIGH_TRANSACTION_VELOCITY");
            return 25;
        } else if (recentTransactions > velocityThreshold / 2) {
            triggeredRules.add("MEDIUM_TRANSACTION_VELOCITY");
            return 10;
        }
        return 0;
    }

    private int checkTransactionTime(List<String> triggeredRules) {
        int hour = LocalDateTime.now().getHour();

        if (hour >= 0 && hour < 5) {
            triggeredRules.add("UNUSUAL_TRANSACTION_HOURS");
            return 20;
        }
        return 0;
    }

    private int checkDuplicateTransactions(PaymentIntent currentPayment, List<String> triggeredRules) {

        List<PaymentIntent> recentSimilar = paymentIntentRepository
                .findRecentSimilarTransactions(
                        currentPayment.getMerchantId(),
                        currentPayment.getAmount(),
                        LocalDateTime.now().minusMinutes(10)
                );

        if (!recentSimilar.isEmpty()) {
            triggeredRules.add("POSSIBLE_DUPLICATE_TRANSACTION");
            return 25;
        }
        return 0;
    }

    private RiskLevel calculateRiskLevel(int riskScore) {
        if (riskScore >= 60) return RiskLevel.HIGH;
        if (riskScore >= 30) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public boolean shouldBlockTransaction(FraudCheck fraudCheck) {
        return fraudCheck.getRiskScore() >= blockRiskScore;
    }

    public Optional<FraudCheck> getFraudCheckBySessionId(String sessionId) {
        return fraudCheckRepository.findByPaymentIntentSessionId(sessionId);
    }
}