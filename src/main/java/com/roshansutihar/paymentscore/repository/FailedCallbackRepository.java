package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.FailedCallback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailedCallbackRepository extends JpaRepository<FailedCallback, Long> {
    List<FailedCallback> findByPaymentIntentId(Long paymentIntentId);

    List<FailedCallback> findByRetryCountGreaterThanAndLastAttemptBefore(Integer retryCount, LocalDateTime threshold);
}
