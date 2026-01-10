package com.roshansutihar.paymentscore.entity;

import com.roshansutihar.paymentscore.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", unique = true, nullable = false)
    private String merchantId;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "callback_url")
    private String callbackUrl;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "commission_type", nullable = false)
    private String commissionType;

    @Column(name = "commission_value", nullable = false)
    private BigDecimal commissionValue;

    @Column(name = "min_commission")
    private BigDecimal minCommission;

    @Column(name = "max_commission")
    private BigDecimal maxCommission;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_routing_number")
    private String bankRoutingNumber;
}