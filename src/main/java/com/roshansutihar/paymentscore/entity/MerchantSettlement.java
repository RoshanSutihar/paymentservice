package com.roshansutihar.paymentscore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_settlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "settlement_reference", unique = true, nullable = false)
    private String settlementReference;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}