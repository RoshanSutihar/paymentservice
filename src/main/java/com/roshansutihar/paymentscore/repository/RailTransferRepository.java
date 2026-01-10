package com.roshansutihar.paymentscore.repository;

import com.roshansutihar.paymentscore.entity.RailTransfer;
import com.roshansutihar.paymentscore.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RailTransferRepository extends JpaRepository<RailTransfer, Long> {
    List<RailTransfer> findByPaymentIntentId(Long paymentIntentId);

    Optional<RailTransfer> findByPaymentIntentIdAndStatus(Long paymentIntentId, TransferStatus status);

    List<RailTransfer> findByStatus(TransferStatus status);

    List<RailTransfer> findByPaymentIntentIdIn(List<Long> paymentIntentIds);
    Optional<RailTransfer> findFirstByPaymentIntentId(Long paymentIntentId);
}
