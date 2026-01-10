package com.roshansutihar.paymentscore.service;


import com.roshansutihar.paymentscore.entity.LedgerEntry;
import com.roshansutihar.paymentscore.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public void createLedgerEntry(Long paymentIntentId, String accountNumber,
                                  String entryType, BigDecimal amount, String description,
                                  String sourceRoutingNumber) {  // NEW: Added parameter for source routing

        BigDecimal currentBalance = getCurrentBalance(accountNumber);
        BigDecimal newBalance = calculateNewBalance(currentBalance, entryType, amount);

        LedgerEntry entry = new LedgerEntry();
        entry.setPaymentIntentId(paymentIntentId);
        entry.setAccountNumber(accountNumber);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setBalanceAfter(newBalance);
        entry.setDescription(description);
        entry.setReferenceId("TXN_" + paymentIntentId);
        entry.setSourceRoutingNumber(sourceRoutingNumber);  // NEW: Set the source routing number

        ledgerEntryRepository.save(entry);
        log.info("Created ledger entry: {} {} for account: {}, new balance: {}, source: {}",
                entryType, amount, accountNumber, newBalance, sourceRoutingNumber);
    }

    // OVERLOAD: Keep old signature for backward compatibility (pass null for sourceRoutingNumber)
    @Transactional
    public void createLedgerEntry(Long paymentIntentId, String accountNumber,
                                  String entryType, BigDecimal amount, String description) {
        createLedgerEntry(paymentIntentId, accountNumber, entryType, amount, description, null);
    }

    public BigDecimal getCurrentBalance(String accountNumber) {
        BigDecimal totalCredits = ledgerEntryRepository.getTotalCredits(accountNumber)
                .orElse(BigDecimal.ZERO);
        BigDecimal totalDebits = ledgerEntryRepository.getTotalDebits(accountNumber)
                .orElse(BigDecimal.ZERO);

        return totalCredits.subtract(totalDebits);
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, String entryType, BigDecimal amount) {
        if ("CREDIT".equals(entryType)) {
            return currentBalance.add(amount);
        } else if ("DEBIT".equals(entryType)) {
            return currentBalance.subtract(amount);
        }
        return currentBalance;
    }

    public List<LedgerEntry> getAccountStatement(String accountNumber) {
        return ledgerEntryRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }
}