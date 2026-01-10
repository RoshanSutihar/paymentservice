package com.roshansutihar.paymentscore.response;

import com.roshansutihar.paymentscore.dtos.SettlementResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSettlementResponse {
    private String merchantId;
    private SettlementResponse settlement;
}
