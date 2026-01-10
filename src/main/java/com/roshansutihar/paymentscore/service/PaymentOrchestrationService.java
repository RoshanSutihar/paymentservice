package com.roshansutihar.paymentscore.service;

import com.roshansutihar.paymentscore.entity.*;
import com.roshansutihar.paymentscore.enums.PaymentStatus;
import com.roshansutihar.paymentscore.enums.TransferStatus;
import com.roshansutihar.paymentscore.entity.MerchantAccount;
import com.roshansutihar.paymentscore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final RailTransferRepository railTransferRepository;
    private final TransactionCommissionRepository transactionCommissionRepository;
    private final CommissionService commissionService;
    private final LedgerService ledgerService;

    @Transactional
    public PaymentIntent createPaymentIntent(String merchantId, String terminalId,
                                             BigDecimal amount, String transactionRef,
                                             String callbackUrl) {

        log.info("Creating payment intent for merchant: {}, terminal: {}, amount: {}",
                merchantId, terminalId, amount);

        MerchantAccount merchant = merchantAccountRepository
                .findByMerchantIdAndStatus(merchantId, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Merchant not found or inactive: " + merchantId));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount: " + amount);
        }

        String sessionId = generateSessionId();

        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setMerchantId(merchantId);
        paymentIntent.setTerminalId(terminalId);
        paymentIntent.setSessionId(sessionId);
        paymentIntent.setAmount(amount);
        paymentIntent.setCurrency("USD");
        paymentIntent.setStatus(PaymentStatus.PENDING);
        paymentIntent.setExpiryTime(LocalDateTime.now().plusMinutes(15)); // 15 min expiry
        paymentIntent.setTransactionRef(transactionRef);
        paymentIntent.setCallbackUrl(callbackUrl != null ? callbackUrl : merchant.getCallbackUrl());

        PaymentIntent savedIntent = paymentIntentRepository.save(paymentIntent);

        recordPaymentEvent(savedIntent.getId(), "CREATED",
                String.format("Payment intent created for amount: %s USD", amount));

        log.info("Successfully created payment intent: {} with session: {}",
                savedIntent.getId(), sessionId);

        return savedIntent;
    }

    public PaymentIntent verifyPaymentSession(String sessionId) {
        log.info("Verifying payment session: {}", sessionId);

        PaymentIntent paymentIntent = paymentIntentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Invalid session ID: " + sessionId));

        if (paymentIntent.isExpired()) {
            paymentIntent.setStatus(PaymentStatus.CANCELLED);
            paymentIntentRepository.save(paymentIntent);
            recordPaymentEvent(paymentIntent.getId(), "EXPIRED", "Payment session expired");
            throw new RuntimeException("Payment session has expired");
        }

        if (paymentIntent.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment already processed with status: " + paymentIntent.getStatus());
        }

        recordPaymentEvent(paymentIntent.getId(), "VERIFIED", "Payment session verified");

        return paymentIntent;
    }

    @Transactional
    public PaymentIntent processPaymentCompletion(String sessionId, String fromAccount, String merchantId, BigDecimal grossAmount, String sourceRoutingNumber) {
        log.info("Processing payment completion for session: {}, from account: {}, merchant: {}, amount: {}, sourceRouting: {}",
                sessionId, fromAccount, merchantId, grossAmount, sourceRoutingNumber);

        PaymentIntent paymentIntent = paymentIntentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found for session: " + sessionId));

        if (paymentIntent.isExpired()) {
            throw new RuntimeException("Payment session expired");
        }

        if (paymentIntent.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment already processed with status: " + paymentIntent.getStatus());
        }

        MerchantAccount merchant = merchantAccountRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        CommissionService.CommissionCalculation commissionCalc =
                commissionService.calculateCommission(merchant, grossAmount);

        TransactionCommission commission = TransactionCommission.builder()
                .paymentIntentId(paymentIntent.getId())
                .transactionAmount(commissionCalc.getTransactionAmount())
                .commissionAmount(commissionCalc.getCommissionAmount())
                .netAmount(commissionCalc.getNetAmount())
                .commissionRate(commissionCalc.getCommissionRate())
                .commissionType(commissionCalc.getCommissionType())
                .build();

        transactionCommissionRepository.save(commission);

        paymentIntent.setAmount(grossAmount);  // Ensure amount is set/updated
        paymentIntent.setStatus(PaymentStatus.COMPLETED);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        PaymentIntent updatedIntent = paymentIntentRepository.save(paymentIntent);

        RailTransfer railTransfer = createRailTransfer(updatedIntent, fromAccount);

        recordLedgerEntries(updatedIntent, commission, fromAccount, sourceRoutingNumber);

        recordPaymentEvent(updatedIntent.getId(), "COMPLETED", "Payment completed successfully");
        recordPaymentEvent(updatedIntent.getId(), "COMMISSION_CALCULATED",
                String.format("Commission: %s, Net: %s",
                        commission.getCommissionAmount(), commission.getNetAmount()));

        log.info("Successfully completed payment for session: {}, amount: {}",
                sessionId, grossAmount);

        return updatedIntent;
    }

    @Transactional
    public PaymentIntent cancelPaymentIntent(String sessionId, String reason) {
        log.info("Cancelling payment intent: {}, reason: {}", sessionId, reason);

        PaymentIntent paymentIntent = paymentIntentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found: " + sessionId));

        if (paymentIntent.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Cannot cancel payment with status: " + paymentIntent.getStatus());
        }

        paymentIntent.setStatus(PaymentStatus.CANCELLED);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        PaymentIntent cancelledIntent = paymentIntentRepository.save(paymentIntent);

        recordPaymentEvent(cancelledIntent.getId(), "CANCELLED",
                String.format("Payment cancelled: %s", reason));

        return cancelledIntent;
    }

    public PaymentIntent getPaymentIntentBySessionId(String sessionId) {
        return paymentIntentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found: " + sessionId));
    }

    public PaymentIntent getPaymentIntentByTransactionRef(String transactionRef) {
        return paymentIntentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Payment intent not found for ref: " + transactionRef));
    }

    // ADD THIS NEW METHOD FOR FRAUD DETECTION
    @Transactional
    public PaymentIntent updatePaymentStatus(String sessionId, PaymentStatus status, String reason) {
        log.info("Updating payment status for session: {} to {} with reason: {}",
                sessionId, status, reason);

        PaymentIntent paymentIntent = paymentIntentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found: " + sessionId));

        // Validate status transition
        validateStatusTransition(paymentIntent.getStatus(), status);

        paymentIntent.setStatus(status);
        paymentIntent.setUpdatedAt(LocalDateTime.now());
        PaymentIntent updatedIntent = paymentIntentRepository.save(paymentIntent);

        // Record the status change event
        recordPaymentEvent(updatedIntent.getId(),
                status.name() + "_BY_FRAUD_SYSTEM",
                String.format("Status updated to %s: %s", status, reason));

        log.info("Successfully updated payment status for session: {} to {}",
                sessionId, status);

        return updatedIntent;
    }

    private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        // Define allowed status transitions
        Map<PaymentStatus, List<PaymentStatus>> allowedTransitions = Map.of(
                PaymentStatus.PENDING, List.of(PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.COMPLETED),
                PaymentStatus.FAILED, List.of(), // Once failed, can't change
                PaymentStatus.CANCELLED, List.of(), // Once cancelled, can't change
                PaymentStatus.COMPLETED, List.of() // Once completed, can't change
        );

        List<PaymentStatus> allowed = allowedTransitions.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new RuntimeException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    private void recordLedgerEntries(PaymentIntent paymentIntent,
                                     TransactionCommission commission,
                                     String fromAccount,
                                     String sourceRoutingNumber) {

        String merchantAccount = "MERCHANT_" + paymentIntent.getMerchantId();
        String systemCommissionAccount = "SYSTEM_COMMISSION";

        ledgerService.createLedgerEntry(
                paymentIntent.getId(),
                fromAccount,
                "DEBIT",
                paymentIntent.getAmount(),
                String.format("Payment to merchant %s - %s",
                        paymentIntent.getMerchantId(), paymentIntent.getTransactionRef()),
                sourceRoutingNumber
        );

        ledgerService.createLedgerEntry(
                paymentIntent.getId(),
                merchantAccount,
                "CREDIT",
                commission.getNetAmount(),
                String.format("Payment received from customer - Ref: %s",
                        paymentIntent.getTransactionRef()),
                sourceRoutingNumber
        );

        if (commission.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.createLedgerEntry(
                    paymentIntent.getId(),
                    systemCommissionAccount,
                    "CREDIT",
                    commission.getCommissionAmount(),
                    String.format("Commission from merchant %s - Ref: %s",
                            paymentIntent.getMerchantId(), paymentIntent.getTransactionRef()),
                    sourceRoutingNumber
            );
        }
    }

    private RailTransfer createRailTransfer(PaymentIntent paymentIntent, String fromAccount) {
        String merchantAccount = "MERCHANT_" + paymentIntent.getMerchantId();

        RailTransfer railTransfer = new RailTransfer();
        railTransfer.setPaymentIntent(paymentIntent);
        railTransfer.setFromAccount(fromAccount);
        railTransfer.setToAccount(merchantAccount);
        railTransfer.setAmount(paymentIntent.getAmount());
        railTransfer.setStatus(TransferStatus.ACKNOWLEDGED);
        railTransfer.setSettlementDate(LocalDateTime.now());
        railTransfer.setResponsePayload("{\"status\": \"success\", \"message\": \"Transfer acknowledged\"}");

        return railTransferRepository.save(railTransfer);
    }

    private void recordPaymentEvent(Long paymentIntentId, String eventType, String message) {
        PaymentIntent paymentIntent = paymentIntentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found: " + paymentIntentId));

        PaymentEvent event = new PaymentEvent();
        event.setPaymentIntent(paymentIntent);
        event.setEventType(eventType);
        event.setPayload(String.format("{\"message\": \"%s\", \"timestamp\": \"%s\"}",
                message, LocalDateTime.now()));
        paymentEventRepository.save(event);

        log.debug("Recorded payment event: {} for payment intent: {}", eventType, paymentIntentId);
    }

    private String generateSessionId() {
        return "SESS_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }
}