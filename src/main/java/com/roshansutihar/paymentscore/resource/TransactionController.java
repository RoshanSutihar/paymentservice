package com.roshansutihar.paymentscore.resource;

import com.roshansutihar.paymentscore.dtos.MerchantTransactionDetailDTO;
import com.roshansutihar.paymentscore.response.MerchantTransactionsResponse;
import com.roshansutihar.paymentscore.response.TransactionSummaryResponse;
import com.roshansutihar.paymentscore.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<MerchantTransactionsResponse> getMerchantTransactions(
            @PathVariable String merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String status) {

        log.info("Fetching transactions for merchant: {} from {} to {}", merchantId, from, to);

        try {
            MerchantTransactionsResponse response = transactionQueryService
                    .getMerchantTransactions(merchantId, from, to, status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch transactions for merchant: {}", merchantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/merchant/{merchantId}/summary")
    public ResponseEntity<TransactionSummaryResponse> getTransactionSummary(
            @PathVariable String merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("Fetching transaction summary for merchant: {} from {} to {}", merchantId, from, to);

        try {
            TransactionSummaryResponse response = transactionQueryService
                    .getTransactionSummary(merchantId, from, to);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch transaction summary for merchant: {}", merchantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/merchant/{merchantId}/today")
    public ResponseEntity<MerchantTransactionsResponse> getTodayTransactions(
            @PathVariable String merchantId) {

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        try {
            MerchantTransactionsResponse response = transactionQueryService
                    .getMerchantTransactions(merchantId, startOfDay, endOfDay, null);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch today's transactions for merchant: {}", merchantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}