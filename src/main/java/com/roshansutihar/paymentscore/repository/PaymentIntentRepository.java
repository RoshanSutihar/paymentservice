package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.PaymentIntent;
import com.roshansutihar.paymentscore.entity.TransactionCommission;
import com.roshansutihar.paymentscore.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    Optional<PaymentIntent> findBySessionId(String sessionId);

    Optional<PaymentIntent> findByTransactionRef(String transactionRef);

    List<PaymentIntent> findByMerchantIdAndStatus(String merchantId, PaymentStatus status);

    @Query("SELECT pi FROM PaymentIntent pi WHERE pi.merchantId = :merchantId AND pi.expiryTime > :now AND pi.status = 'PENDING'")
    List<PaymentIntent> findPendingByMerchantIdAndNotExpired(@Param("merchantId") String merchantId, @Param("now") LocalDateTime now);

    List<PaymentIntent> findByMerchantId(String merchantId);


    List<PaymentIntent> findByMerchantIdAndCreatedAtBetween(String merchantId,
                                                            LocalDateTime startDate,
                                                            LocalDateTime endDate);

    @Query("SELECT pi FROM PaymentIntent pi WHERE pi.merchantId = :merchantId " +
            "AND DATE(pi.createdAt) = :date")
    List<PaymentIntent> findByMerchantIdAndDate(@Param("merchantId") String merchantId,
                                                @Param("date") LocalDate date);

    @Query("SELECT p FROM PaymentIntent p WHERE p.merchantId = :merchantId AND p.amount = :amount AND p.createdAt >= :since AND p.status = 'COMPLETED'")
    List<PaymentIntent> findRecentSimilarTransactions(@Param("merchantId") String merchantId,
                                                      @Param("amount") BigDecimal amount,
                                                      @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM PaymentIntent p WHERE p.merchantId = :merchantId AND p.createdAt >= :since")
    Long countRecentTransactionsByMerchant(@Param("merchantId") String merchantId,
                                           @Param("since") LocalDateTime since);
}


