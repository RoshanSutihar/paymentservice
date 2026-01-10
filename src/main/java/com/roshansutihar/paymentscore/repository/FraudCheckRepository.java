package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.FraudCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {

    Optional<FraudCheck> findByPaymentIntentSessionId(String sessionId);

    @Query("SELECT fc FROM FraudCheck fc WHERE fc.paymentIntent.id = :paymentIntentId")
    Optional<FraudCheck> findByPaymentIntentId(@Param("paymentIntentId") Long paymentIntentId);
}
