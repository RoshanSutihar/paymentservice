package com.roshansutihar.paymentscore.resource;

import com.roshansutihar.paymentscore.entity.MerchantAccount;
import com.roshansutihar.paymentscore.entity.MerchantSettlement;
import com.roshansutihar.paymentscore.request.MerchantRegistrationRequest;
import com.roshansutihar.paymentscore.request.MerchantUpdateRequest;
import com.roshansutihar.paymentscore.response.MerchantProfileResponse;
import com.roshansutihar.paymentscore.response.MerchantRegistrationResponse;
import com.roshansutihar.paymentscore.response.MerchantUpdateResponse;
import com.roshansutihar.paymentscore.response.SecretRotationResponse;
import com.roshansutihar.paymentscore.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/merchants")
@Slf4j
@RequiredArgsConstructor
@Validated
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/register")
    public ResponseEntity<MerchantRegistrationResponse> registerMerchant(
            @Valid @RequestBody MerchantRegistrationRequest request) {

        try {
            MerchantAccount merchant = merchantService.registerMerchant(request);


            MerchantRegistrationResponse response = MerchantRegistrationResponse.builder()
                    .success(true)
                    .merchantId(merchant.getMerchantId())
                    .storeName(merchant.getStoreName())
                    .secretKey(merchant.getSecretKey())
                    .callbackUrl(merchant.getCallbackUrl())
                    .status(String.valueOf(merchant.getStatus()))
                    .commissionType(String.valueOf(merchant.getCommissionType()))
                    .commissionValue(merchant.getCommissionValue())
                    .message("Merchant registered successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            MerchantRegistrationResponse response = MerchantRegistrationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantProfileResponse> getMerchantProfile(@PathVariable String merchantId) {


        try {
            MerchantAccount merchant = merchantService.getMerchant(merchantId);

            MerchantProfileResponse response = MerchantProfileResponse.builder()
                    .merchantId(merchant.getMerchantId())
                    .storeName(merchant.getStoreName())
                    .callbackUrl(merchant.getCallbackUrl())
                    .status(String.valueOf(merchant.getStatus()))
                    .commissionType(String.valueOf(merchant.getCommissionType()))
                    .commissionValue(merchant.getCommissionValue())
                    .minCommission(merchant.getMinCommission())
                    .maxCommission(merchant.getMaxCommission())
                    .createdAt(merchant.getCreatedAt())
                    .bankAccountNumber(merchant.getBankAccountNumber())
                    .bankRoutingNumber(merchant.getBankRoutingNumber())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch merchant profile: {}", merchantId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{merchantId}")
    public ResponseEntity<MerchantUpdateResponse> updateMerchant(
            @PathVariable String merchantId,
            @Valid @RequestBody MerchantUpdateRequest request) {

        log.info("Updating merchant profile: {}", merchantId);

        try {
            MerchantAccount merchant = merchantService.updateMerchant(merchantId, request);

            MerchantUpdateResponse response = MerchantUpdateResponse.builder()
                    .success(true)
                    .merchantId(merchant.getMerchantId())
                    .storeName(merchant.getStoreName())
                    .callbackUrl(merchant.getCallbackUrl())
                    .status(String.valueOf(merchant.getStatus()))
                    .message("Merchant updated successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Merchant update failed: {}", merchantId, e);

            MerchantUpdateResponse response = MerchantUpdateResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{merchantId}/rotate-secret")
    public ResponseEntity<SecretRotationResponse> rotateSecretKey(@PathVariable String merchantId) {
        log.info("Rotating secret key for merchant: {}", merchantId);

        try {
            String newSecretKey = merchantService.rotateSecretKey(merchantId);

            SecretRotationResponse response = SecretRotationResponse.builder()
                    .success(true)
                    .merchantId(merchantId)
                    .newSecretKey(newSecretKey)
                    .message("Secret key rotated successfully")
                    .build();

            log.info("Secret key rotated for merchant: {}", merchantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Secret key rotation failed for merchant: {}", merchantId, e);

            SecretRotationResponse response = SecretRotationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/ids")
    public ResponseEntity<List<String>> getAllMerchantIds() {
        try {
            List<MerchantAccount> merchants = merchantService.getAllMerchants();


            List<String> merchantIds = merchants.stream()
                    .map(MerchantAccount::getMerchantId)
                    .collect(Collectors.toList());

            log.info("Returning {} merchant IDs", merchantIds.size());
            return ResponseEntity.ok(merchantIds);

        } catch (Exception e) {
            log.error("Failed to fetch merchant IDs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<MerchantProfileResponse>> getAllMerchants() {
        try {
            List<MerchantAccount> merchants = merchantService.getAllMerchants();

            List<MerchantProfileResponse> response = merchants.stream()
                    .map(merchant -> MerchantProfileResponse.builder()
                            .merchantId(merchant.getMerchantId())
                            .storeName(merchant.getStoreName())
                            .callbackUrl(merchant.getCallbackUrl())
                            .status(String.valueOf(merchant.getStatus()))
                            .commissionType(String.valueOf(merchant.getCommissionType()))
                            .commissionValue(merchant.getCommissionValue())
                            .createdAt(merchant.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch merchants", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}