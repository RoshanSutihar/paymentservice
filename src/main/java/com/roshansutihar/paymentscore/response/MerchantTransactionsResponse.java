package com.roshansutihar.paymentscore.response;

import com.roshansutihar.paymentscore.dtos.MerchantTransactionDTO;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MerchantTransactionsResponse {
    private String merchantId;
    private List<MerchantTransactionDTO> transactions;
    private Integer totalCount;
    @Getter
    private BigDecimal totalAmount;
}
