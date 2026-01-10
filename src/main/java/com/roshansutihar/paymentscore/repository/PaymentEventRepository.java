package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPaymentIntentIdOrderByCreatedAtDesc(Long paymentIntentId);

    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.paymentIntent.id = :paymentIntentId AND pe.createdAt >= :from AND pe.createdAt <= :to")
    List<PaymentEvent> findByPaymentIntentIdBetweenDates(@Param("paymentIntentId") Long paymentIntentId,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    List<PaymentEvent> findByPaymentIntentId(Long paymentIntentId);
}
