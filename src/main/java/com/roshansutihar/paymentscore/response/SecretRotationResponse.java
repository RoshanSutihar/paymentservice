package com.roshansutihar.paymentscore.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretRotationResponse {
    private boolean success;
    private String merchantId;
    private String newSecretKey;
    private String message;
}
