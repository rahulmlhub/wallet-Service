package com.wallet.payload;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositWithdrawRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000000", message = "Amount cannot exceed 1,000,000,000")
    @Digits(integer = 10, fraction = 4, message = "Amount must have at most 10 integer digits and 4 decimal digits")
    private BigDecimal amount;

    @Size(max = 100, message = "Reference ID cannot exceed 100 characters")
    private String referenceId;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}