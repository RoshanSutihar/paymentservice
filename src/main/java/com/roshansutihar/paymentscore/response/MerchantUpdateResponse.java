package com.roshansutihar.paymentscore.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantUpdateResponse {
    private boolean success;
    private String merchantId;
    private String storeName;
    private String callbackUrl;
    private String status;
    private String message;
}
