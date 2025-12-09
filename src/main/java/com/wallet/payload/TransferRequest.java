package com.wallet.payload;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {
    @NotNull(message = "From wallet ID is required")
    private UUID fromWalletId;

    @NotNull(message = "To wallet ID is required")
    private UUID toWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000000", message = "Amount cannot exceed 1,000,000,000")
    private BigDecimal amount;

//    @Size(max = 100, message = "Reference ID cannot exceed 100 characters")
    private String referenceId;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}

