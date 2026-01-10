package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.TransactionCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionCommissionRepository extends JpaRepository<TransactionCommission, Long> {
    Optional<TransactionCommission> findByPaymentIntentId(Long paymentIntentId);
    @Query("SELECT SUM(tc.commissionAmount) FROM TransactionCommission tc " +
            "JOIN PaymentIntent pi ON tc.paymentIntentId = pi.id " +
            "WHERE pi.merchantId = :merchantId " +
            "AND pi.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal findTotalCommissionByMerchantAndDateRange(@Param("merchantId") String merchantId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    List<TransactionCommission> findByPaymentIntentIdIn(List<Long> paymentIntentIds);
}
