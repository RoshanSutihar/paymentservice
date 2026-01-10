package com.roshansutihar.paymentscore.service;


import com.roshansutihar.paymentscore.entity.MerchantAccount;
import com.roshansutihar.paymentscore.entity.MerchantSettlement;
import com.roshansutihar.paymentscore.enums.MerchantStatus;
import com.roshansutihar.paymentscore.repository.MerchantAccountRepository;
import com.roshansutihar.paymentscore.request.MerchantRegistrationRequest;
import com.roshansutihar.paymentscore.request.MerchantUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantAccountRepository merchantAccountRepository;


    public String getMerchantSecret(String merchantId) {
        MerchantAccount merchant = merchantAccountRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + merchantId));
        return merchant.getSecretKey();
    }


    public MerchantAccount getMerchant(String merchantId) {
        return merchantAccountRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + merchantId));
    }


    public Optional<MerchantAccount> getActiveMerchant(String merchantId) {
        return merchantAccountRepository.findByMerchantIdAndStatus(merchantId, "ACTIVE");
    }



    public MerchantAccount registerMerchant(MerchantRegistrationRequest request) {
        String merchantId = generateMerchantId(request.getStoreName());

        if (merchantAccountRepository.existsByMerchantId(merchantId)) {
            throw new RuntimeException("Merchant ID already exists: " + merchantId);
        }

        String secretKey = generateSecretKey();

        MerchantAccount merchant = MerchantAccount.builder()
                .merchantId(merchantId)
                .storeName(request.getStoreName())
                .secretKey(secretKey)
                .callbackUrl(request.getCallbackUrl())
                .status("ACTIVE")
                .commissionType(request.getCommissionType() != null ? request.getCommissionType() : "PERCENTAGE")
                .commissionValue(request.getCommissionValue() != null ? request.getCommissionValue() : new BigDecimal("0.02"))
                .minCommission(request.getMinCommission() != null ? request.getMinCommission() : BigDecimal.ZERO)
                .maxCommission(request.getMaxCommission())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankRoutingNumber(request.getBankRoutingNumber())
                .build();

        return merchantAccountRepository.save(merchant);
    }

    public MerchantAccount updateMerchant(String merchantId, MerchantUpdateRequest request) {
        MerchantAccount merchant = getMerchant(merchantId);

        if (request.getStoreName() != null) {
            merchant.setStoreName(request.getStoreName());
        }
        if (request.getCallbackUrl() != null) {
            merchant.setCallbackUrl(request.getCallbackUrl());
        }
        if (request.getStatus() != null) {
            merchant.setStatus(request.getStatus());
        }
        if (request.getCommissionType() != null) {
            merchant.setCommissionType(request.getCommissionType());
        }
        if (request.getCommissionValue() != null) {
            merchant.setCommissionValue(request.getCommissionValue());
        }
        if (request.getMinCommission() != null) {
            merchant.setMinCommission(request.getMinCommission());
        }
        if (request.getMaxCommission() != null) {
            merchant.setMaxCommission(request.getMaxCommission());
        }

        return merchantAccountRepository.save(merchant);
    }

    public String rotateSecretKey(String merchantId) {
        MerchantAccount merchant = getMerchant(merchantId);
        String newSecretKey = generateSecretKey();
        merchant.setSecretKey(newSecretKey);
        merchantAccountRepository.save(merchant);
        return newSecretKey;
    }

    public List<MerchantAccount> getAllMerchants() {
        return merchantAccountRepository.findAll();
    }

    private String generateMerchantId(String storeName) {

        String base = storeName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_");
        return base + "_" + System.currentTimeMillis();
    }
    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}