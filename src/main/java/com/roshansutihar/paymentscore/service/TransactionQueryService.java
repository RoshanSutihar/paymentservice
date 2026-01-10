package com.roshansutihar.paymentscore.service;

import com.roshansutihar.paymentscore.dtos.*;
import com.roshansutihar.paymentscore.entity.*;
import com.roshansutihar.paymentscore.enums.PaymentStatus;
import com.roshansutihar.paymentscore.repository.*;
import com.roshansutihar.paymentscore.response.MerchantTransactionsResponse;
import com.roshansutihar.paymentscore.response.TransactionSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionQueryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final RailTransferRepository railTransferRepository;
    private final TransactionCommissionRepository transactionCommissionRepository;
    private final PaymentIntentRepository paymentIntentRepository;

    public MerchantTransactionsResponse getMerchantTransactions(String merchantId,
                                                                LocalDateTime startDate,
                                                                LocalDateTime endDate,
                                                                String status) {

        String merchantAccount = "MERCHANT_" + merchantId;
        List<LedgerEntry> creditEntries = ledgerEntryRepository
                .findByAccountNumberAndEntryTypeAndCreatedAtBetween(
                        merchantAccount, "CREDIT", startDate, endDate);


        if (status != null && !status.isEmpty()) {
            creditEntries = creditEntries.stream()
                    .filter(entry -> {

                        List<RailTransfer> railTransfers = railTransferRepository
                                .findByPaymentIntentId(entry.getPaymentIntentId());
                        return !railTransfers.isEmpty() &&
                                railTransfers.get(0).getStatus().name().equals(status);
                    })
                    .collect(Collectors.toList());
        }

        List<MerchantTransactionDTO> transactions = creditEntries.stream()
                .map(this::mapLedgerToTransactionDTO)
                .collect(Collectors.toList());

        return MerchantTransactionsResponse.builder()
                .merchantId(merchantId)
                .transactions(transactions)
                .totalCount(transactions.size())
                .totalAmount(calculateTotalAmount(transactions))
                .build();
    }

    private MerchantTransactionDTO mapLedgerToTransactionDTO(LedgerEntry ledgerEntry) {

        PaymentIntent paymentIntent = paymentIntentRepository
                .findById(ledgerEntry.getPaymentIntentId())
                .orElse(null);


        Optional<TransactionCommission> commission = transactionCommissionRepository
                .findByPaymentIntentId(ledgerEntry.getPaymentIntentId());


        List<RailTransfer> railTransfers = railTransferRepository
                .findByPaymentIntentId(ledgerEntry.getPaymentIntentId());
        RailTransfer railTransfer = railTransfers.isEmpty() ? null : railTransfers.get(0);


        BigDecimal originalAmount = paymentIntent != null ? paymentIntent.getAmount() : ledgerEntry.getAmount();
        BigDecimal commissionAmount = commission.map(TransactionCommission::getCommissionAmount)
                .orElse(BigDecimal.ZERO);
        BigDecimal netAmount = ledgerEntry.getAmount(); // This is what actually went to merchant

        return MerchantTransactionDTO.builder()
                .sessionId(paymentIntent != null ? paymentIntent.getSessionId() : "N/A")
                .transactionRef(paymentIntent != null ? paymentIntent.getTransactionRef() : "N/A")
                .amount(originalAmount)
                .currency(paymentIntent != null ? paymentIntent.getCurrency() : "USD")
                .status(railTransfer != null ? railTransfer.getStatus().name() : "UNKNOWN")
                .createdAt(ledgerEntry.getCreatedAt())
                .completedAt(railTransfer != null ? railTransfer.getSettlementDate() : null)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .settlementDate(railTransfer != null ? railTransfer.getSettlementDate() : null)
                .build();
    }

    public TransactionSummaryResponse getTransactionSummary(String merchantId,
                                                            LocalDateTime startDate,
                                                            LocalDateTime endDate) {

        String merchantAccount = "MERCHANT_" + merchantId;


        List<LedgerEntry> settledTransactions = ledgerEntryRepository
                .findByAccountNumberAndEntryTypeAndCreatedAtBetween(
                        merchantAccount, "CREDIT", startDate, endDate);


        List<Long> paymentIntentIds = settledTransactions.stream()
                .map(LedgerEntry::getPaymentIntentId)
                .collect(Collectors.toList());


        List<RailTransfer> railTransfers = railTransferRepository
                .findByPaymentIntentIdIn(paymentIntentIds);

        Map<String, Long> statusCount = railTransfers.stream()
                .collect(Collectors.groupingBy(rt -> rt.getStatus().name(), Collectors.counting()));

        List<PaymentIntent> paymentIntents = paymentIntentRepository
                .findAllById(paymentIntentIds);

        BigDecimal totalGrossAmount = paymentIntents.stream()
                .map(PaymentIntent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = calculateTotalCommission(paymentIntentIds);

        BigDecimal totalNetAmount = totalGrossAmount.subtract(totalCommission);

        return TransactionSummaryResponse.builder()
                .merchantId(merchantId)
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalTransactions(settledTransactions.size())
                .totalAmount(totalGrossAmount)
                .totalNetAmount(totalNetAmount)
                .totalCommission(totalCommission)
                .statusBreakdown(statusCount)
                .build();
    }

    private BigDecimal calculateTotalAmount(List<MerchantTransactionDTO> transactions) {
        return transactions.stream()
                .map(MerchantTransactionDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private BigDecimal calculateTotalCommission(List<Long> paymentIntentIds) {
        if (paymentIntentIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<TransactionCommission> commissions = transactionCommissionRepository
                .findByPaymentIntentIdIn(paymentIntentIds);

        return commissions.stream()
                .map(TransactionCommission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}