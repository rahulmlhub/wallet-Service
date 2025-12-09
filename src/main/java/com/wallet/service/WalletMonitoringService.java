package com.wallet.service;

import com.wallet.model.Wallet;
import com.wallet.payload.WalletDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletMonitoringService {
    List<WalletDTO> getWalletsByUser(UUID userId);
    List<WalletDTO> getWalletsByStatus(Wallet.WalletStatus status);
    BigDecimal getTotalSystemBalance();
    int getActiveWalletCount();
    int getTransactionCount(UUID walletId);
}
