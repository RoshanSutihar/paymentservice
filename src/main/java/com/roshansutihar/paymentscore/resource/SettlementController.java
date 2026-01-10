package com.roshansutihar.paymentscore.resource;

import com.roshansutihar.paymentscore.dtos.MerchantTransactionDTO;
import com.roshansutihar.paymentscore.dtos.SettlementResponse;
import com.roshansutihar.paymentscore.dtos.SettlementTransaction;
import com.roshansutihar.paymentscore.request.SettlementMarkRequest;
import com.roshansutihar.paymentscore.response.BatchSettlementResponse;
import com.roshansutihar.paymentscore.response.MerchantTransactionsResponse;
import com.roshansutihar.paymentscore.response.SettlementMarkResponse;
import com.roshansutihar.paymentscore.service.MerchantService;
import com.roshansutihar.paymentscore.service.TransactionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/settlement")
@Slf4j
@RequiredArgsConstructor
@Validated
public class SettlementController {

    private final TransactionQueryService transactionQueryService;
    private final MerchantService merchantService;

    @GetMapping("/merchant/{merchantId}/pending")
    public ResponseEntity<SettlementResponse> getPendingSettlement(
            @PathVariable String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("Fetching pending settlement for merchant: {}", merchantId);

        try {

            if (from == null) {
                from = getLastSettlementDate(merchantId);
            }
            if (to == null) {
                to = LocalDateTime.now();
            }


            MerchantTransactionsResponse merchantTransactions = transactionQueryService
                    .getMerchantTransactions(merchantId, from, to, "ACKNOWLEDGED");

            if (merchantTransactions.getTransactions().isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            SettlementResponse settlementResponse = buildSettlementResponse(merchantId, merchantTransactions);
            return ResponseEntity.ok(settlementResponse);

        } catch (Exception e) {
            log.error("Failed to fetch pending settlement for merchant: {}", merchantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/merchant/{merchantId}/batch")
    public ResponseEntity<BatchSettlementResponse> getBatchSettlement(
            @PathVariable String merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("Fetching batch settlement for merchant: {} from {} to {}", merchantId, from, to);

        try {

            MerchantTransactionsResponse merchantTransactions = transactionQueryService
                    .getMerchantTransactions(merchantId, from, to, "ACKNOWLEDGED");

            SettlementResponse settlement = buildSettlementResponse(merchantId, merchantTransactions);

            BatchSettlementResponse response = BatchSettlementResponse.builder()
                    .merchantId(merchantId)
                    .settlement(settlement)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch batch settlement for merchant: {}", merchantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/today/pending")
    public ResponseEntity<SettlementResponse> getTodayPendingSettlement(@RequestParam String merchantId) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        try {
            MerchantTransactionsResponse merchantTransactions = transactionQueryService
                    .getMerchantTransactions(merchantId, startOfDay, endOfDay, "ACKNOWLEDGED");

            if (merchantTransactions.getTransactions().isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            SettlementResponse settlementResponse = buildSettlementResponse(merchantId, merchantTransactions);
            return ResponseEntity.ok(settlementResponse);

        } catch (Exception e) {
            log.error("Failed to fetch today's pending settlement for merchant: {}", merchantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/mark-settled")
    public ResponseEntity<SettlementMarkResponse> markSettlementAsProcessed(
            @RequestBody @Valid SettlementMarkRequest request) {

        log.info("Marking settlement as processed: {}", request.getSettlementId());

        try {
            SettlementMarkResponse response = SettlementMarkResponse.builder()
                    .success(true)
                    .settlementId(request.getSettlementId())
                    .message("Settlement marked as processed successfully")
                    .processedAt(LocalDateTime.now())
                    .transactionCount(request.getTransactionRefs().size())
                    .totalAmount(BigDecimal.valueOf(request.getTotalAmount()))
                    .totalNetAmount(BigDecimal.valueOf(request.getTotalNetAmount()))
                    .totalFees(BigDecimal.valueOf(request.getTotalFees()))
                    .build();

            log.info("Settlement {} marked as processed with {} transactions",
                    request.getSettlementId(), request.getTransactionRefs().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to mark settlement as processed: {}", request.getSettlementId(), e);

            SettlementMarkResponse response = SettlementMarkResponse.builder()
                    .success(false)
                    .message("Failed to process settlement: " + e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    private SettlementResponse buildSettlementResponse(String merchantId,
                                                       MerchantTransactionsResponse merchantTransactions) {

        List<SettlementTransaction> settlementTransactions = merchantTransactions.getTransactions().stream()
                .map(this::convertToSettlementTransaction)
                .collect(Collectors.toList());


        BigDecimal totalAmount = settlementTransactions.stream()
                .map(SettlementTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = settlementTransactions.stream()
                .map(SettlementTransaction::getFees)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetAmount = settlementTransactions.stream()
                .map(SettlementTransaction::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String settlementId = generateSettlementId(merchantId);


        LocalDateTime periodFrom = merchantTransactions.getTransactions().stream()
                .map(MerchantTransactionDTO::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime periodTo = merchantTransactions.getTransactions().stream()
                .map(MerchantTransactionDTO::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return SettlementResponse.builder()
                .settlementId(settlementId)
                .merchantId(merchantId)
                .generatedAt(LocalDateTime.now())
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .transactions(settlementTransactions)
                .totalAmount(totalAmount)
                .totalFees(totalFees)
                .totalNetAmount(totalNetAmount)
                .transactionCount(settlementTransactions.size())
                .status("PENDING_SETTLEMENT")
                .build();
    }

    private SettlementTransaction convertToSettlementTransaction(MerchantTransactionDTO transaction) {
        return SettlementTransaction.builder()
                .transactionRef(transaction.getTransactionRef())
                .sessionId(transaction.getSessionId())
                .amount(transaction.getAmount())
                .fees(transaction.getCommissionAmount())
                .netAmount(transaction.getNetAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .settlementDate(transaction.getSettlementDate())
                .build();
    }

    private String generateSettlementId(String merchantId) {
        return "SETTLE_" + merchantId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private LocalDateTime getLastSettlementDate(String merchantId) {

        return LocalDateTime.now().with(LocalTime.MIN);
    }
}
