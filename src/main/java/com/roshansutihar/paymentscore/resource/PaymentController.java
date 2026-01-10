package com.roshansutihar.paymentscore.resource;



import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roshansutihar.paymentscore.entity.FraudCheck;
import com.roshansutihar.paymentscore.entity.PaymentIntent;
import com.roshansutihar.paymentscore.enums.PaymentStatus;
import com.roshansutihar.paymentscore.request.PaymentCancellationRequest;
import com.roshansutihar.paymentscore.request.PaymentCompletionRequest;
import com.roshansutihar.paymentscore.request.PaymentInitiationRequest;
import com.roshansutihar.paymentscore.response.*;
import com.roshansutihar.paymentscore.service.FraudDetectionService;
import com.roshansutihar.paymentscore.service.MerchantService;
import com.roshansutihar.paymentscore.service.PaymentOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = {
        "${BANK_API}",
        "${POS_API}",
        "${MERCHANT_API}"
})

public class PaymentController {

    private final PaymentOrchestrationService paymentOrchestrationService;
    private final MerchantService merchantService;

    private final FraudDetectionService fraudDetectionService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiationRequest request,
            @RequestHeader("X-Merchant-ID") String merchantId,
            @RequestHeader("X-Signature") String signature) {

        log.info("Received payment initiation request from merchant: {}, terminal: {}",
                merchantId, request.getTerminalId());

        try {

            validateSignature(merchantId, signature, request);

            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Invalid amount: " + request.getAmount());
            }

            if (request.getTransactionRef() == null || request.getTransactionRef().trim().isEmpty()) {
                throw new RuntimeException("Transaction reference is required");
            }

            PaymentIntent paymentIntent = paymentOrchestrationService.createPaymentIntent(
                    merchantId,
                    request.getTerminalId(),
                    request.getAmount(),
                    request.getTransactionRef(),
                    request.getCallbackUrl()
            );


            FraudCheck fraudCheck = fraudDetectionService.performFraudCheck(paymentIntent);


            if (fraudDetectionService.shouldBlockTransaction(fraudCheck)) {
                log.warn("Payment blocked by fraud check. Session: {}, Risk: {}, Score: {}, Rules: {}",
                        paymentIntent.getSessionId(), fraudCheck.getRiskLevel(),
                        fraudCheck.getRiskScore(), fraudCheck.getRulesTriggered());


                paymentOrchestrationService.updatePaymentStatus(
                        paymentIntent.getSessionId(),
                        PaymentStatus.FAILED,
                        "Transaction flagged by fraud system"
                );

                throw new RuntimeException("Transaction declined due to security reasons. Please contact support.");
            }


            PaymentInitiationResponse response = PaymentInitiationResponse.builder()
                    .success(true)
                    .sessionId(paymentIntent.getSessionId())
                    .qrData(generateQRData(paymentIntent))
                    .expiryTime(paymentIntent.getExpiryTime())
                    .message("Payment initiated successfully")
                    .build();

            log.info("Payment initiated successfully for merchant: {}, session: {}",
                    merchantId, paymentIntent.getSessionId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Payment initiation failed for merchant: {}", merchantId, e);

            PaymentInitiationResponse response = PaymentInitiationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }


    @GetMapping("/{sessionId}/fraud-check")
    public ResponseEntity<FraudCheckResponse> getFraudCheckStatus(@PathVariable String sessionId) {
        try {
            Optional<FraudCheck> fraudCheck = fraudDetectionService.getFraudCheckBySessionId(sessionId);

            if (fraudCheck.isPresent()) {
                FraudCheck check = fraudCheck.get();
                FraudCheckResponse response = FraudCheckResponse.builder()
                        .riskScore(check.getRiskScore())
                        .riskLevel(check.getRiskLevel())
                        .triggeredRules(check.getRulesTriggered())
                        .createdAt(check.getCreatedAt())
                        .build();
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Failed to get fraud check for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/verify/{sessionId}")
    public ResponseEntity<PaymentVerificationResponse> verifyPaymentSession(
            @PathVariable String sessionId) {

        log.info("Verifying payment session: {}", sessionId);

        try {
            PaymentIntent paymentIntent = paymentOrchestrationService.verifyPaymentSession(sessionId);

            PaymentVerificationResponse response = PaymentVerificationResponse.builder()
                    .valid(true)
                    .sessionId(sessionId)
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .merchantId(paymentIntent.getMerchantId())
                    .status(paymentIntent.getStatus().name())
                    .expiryTime(paymentIntent.getExpiryTime())
                    .message("Session is valid")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Payment session verification failed: {}", sessionId, e);

            PaymentVerificationResponse response = PaymentVerificationResponse.builder()
                    .valid(false)
                    .sessionId(sessionId)
                    .message(e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<PaymentCompletionResponse> completePayment(
            @Valid @RequestBody PaymentCompletionRequest request) {

        log.info("Received payment completion for session: {}, from account: {}",
                request.getSessionId(), request.getFromAccount());

        try {

            String merchantId = request.getMerchantId();
            Double grossAmountDouble = request.getGrossAmount();
            BigDecimal grossAmount = (grossAmountDouble != null) ? BigDecimal.valueOf(grossAmountDouble) : BigDecimal.ZERO;
            String sourceRoutingNumber = request.getSourceRoutingNumber();


            if (merchantId == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0 || sourceRoutingNumber == null) {
                throw new RuntimeException("Missing merchantId, invalid grossAmount, or sourceRoutingNumber");
            }

            PaymentIntent paymentIntent = paymentOrchestrationService.processPaymentCompletion(
                    request.getSessionId(),
                    request.getFromAccount(),
                    merchantId,
                    grossAmount,
                    sourceRoutingNumber
            );

            PaymentCompletionResponse response = PaymentCompletionResponse.builder()
                    .success(true)
                    .transactionId(paymentIntent.getId().toString())
                    .sessionId(paymentIntent.getSessionId())
                    .amount(grossAmount)  // Use gross from request
                    .status(paymentIntent.getStatus().name())
                    .message("Payment completed successfully — ledger entry created")
                    .build();

            log.info("Payment completed successfully for session: {}", request.getSessionId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Payment completion failed for session: {}", request.getSessionId(), e);

            PaymentCompletionResponse response = PaymentCompletionResponse.builder()
                    .success(false)
                    .sessionId(request.getSessionId())
                    .message(e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<PaymentCancellationResponse> cancelPayment(
            @Valid @RequestBody PaymentCancellationRequest request) {

        log.info("Received payment cancellation for session: {}", request.getSessionId());

        try {
            PaymentIntent paymentIntent = paymentOrchestrationService.cancelPaymentIntent(
                    request.getSessionId(),
                    request.getReason()
            );

            PaymentCancellationResponse response = PaymentCancellationResponse.builder()
                    .success(true)
                    .sessionId(paymentIntent.getSessionId())
                    .status(paymentIntent.getStatus().name())
                    .message("Payment cancelled successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Payment cancellation failed for session: {}", request.getSessionId(), e);

            PaymentCancellationResponse response = PaymentCancellationResponse.builder()
                    .success(false)
                    .sessionId(request.getSessionId())
                    .message(e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String sessionId) {

        try {
            PaymentIntent paymentIntent = paymentOrchestrationService.getPaymentIntentBySessionId(sessionId);

            PaymentStatusResponse response = PaymentStatusResponse.builder()
                    .sessionId(sessionId)
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .status(paymentIntent.getStatus().name())
                    .merchantId(paymentIntent.getMerchantId())
                    .createdAt(paymentIntent.getCreatedAt())
                    .expiryTime(paymentIntent.getExpiryTime())
                    .transactionRef(paymentIntent.getTransactionRef())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get payment status for session: {}", sessionId, e);
            return ResponseEntity.notFound().build();
        }
    }

    private void validateSignature(String merchantId, String signature, Object request) {
        try {
            String secretKey = merchantService.getMerchantSecret(merchantId);
            if (secretKey == null) {
                throw new RuntimeException("Merchant not found or invalid");
            }


            ObjectMapper objectMapper = new ObjectMapper();


            objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

            String requestBody = objectMapper.writeValueAsString(request);

            String dataToSign = merchantId + requestBody;


            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] decodedKeyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKeyBytes, "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);


            if (!expectedSignature.equals(signature)) {
                throw new RuntimeException("Invalid signature - expected: " + expectedSignature + " but got: " + signature);
            }

        } catch (Exception e) {
            log.error("Signature validation error", e);
            throw new RuntimeException("Signature validation failed: " + e.getMessage());
        }
    }

    private String generateQRData(PaymentIntent paymentIntent) {
        return String.format("QRPAY|%s|%.2f|%s|%s|%s",
                paymentIntent.getSessionId(),
                paymentIntent.getAmount(),
                paymentIntent.getCurrency(),
                paymentIntent.getMerchantId(),
                paymentIntent.getTransactionRef());
    }
}