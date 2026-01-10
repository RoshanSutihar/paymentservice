package com.roshansutihar.paymentscore.response;

import com.roshansutihar.paymentscore.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FraudCheckResponse {
    private Integer riskScore;
    private RiskLevel riskLevel;
    private List<String> triggeredRules;
    private LocalDateTime createdAt;
}
