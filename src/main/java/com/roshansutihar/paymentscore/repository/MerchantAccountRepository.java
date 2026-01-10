package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.MerchantAccount;
import com.roshansutihar.paymentscore.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantAccountRepository extends JpaRepository<MerchantAccount, Long> {
    Optional<MerchantAccount> findByMerchantId(String merchantId);
    Optional<MerchantAccount> findByMerchantIdAndStatus(String merchantId, String status);
    boolean existsByMerchantId(String merchantId);
}
