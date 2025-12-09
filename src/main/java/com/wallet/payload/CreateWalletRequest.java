package com.wallet.payload;


import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateWalletRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase code")
    @NotBlank(message = "Currency is required")
    private String currency = "USD";
}