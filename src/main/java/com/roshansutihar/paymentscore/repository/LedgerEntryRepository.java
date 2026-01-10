package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByPaymentIntentIdOrderByCreatedAtDesc(Long paymentIntentId);
    List<LedgerEntry> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.accountNumber = :accountNumber AND le.entryType = 'CREDIT'")
    Optional<BigDecimal> getTotalCredits(@Param("accountNumber") String accountNumber);

    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.accountNumber = :accountNumber AND le.entryType = 'DEBIT'")
    Optional<BigDecimal> getTotalDebits(@Param("accountNumber") String accountNumber);

    List<LedgerEntry> findByAccountNumberAndEntryTypeAndCreatedAtBetween(
            String accountNumber, String entryType, LocalDateTime start, LocalDateTime end);

    List<LedgerEntry> findByAccountNumberAndEntryType(String accountNumber, String entryType);


}