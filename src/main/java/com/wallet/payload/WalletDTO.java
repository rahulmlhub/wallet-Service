package com.wallet.payload;

import com.wallet.model.Wallet;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WalletDTO {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
    private String currency;
    private Wallet.WalletStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WalletDTO fromEntity(Wallet wallet) {
        WalletDTO dto = new WalletDTO();
        dto.setId(wallet.getId());
        dto.setUserId(wallet.getUserId());
        dto.setBalance(wallet.getBalance());
        dto.setCurrency(wallet.getCurrency());
        dto.setStatus(wallet.getStatus());
        dto.setCreatedAt(wallet.getCreatedAt());
        dto.setUpdatedAt(wallet.getUpdatedAt());
        return dto;
    }
}
