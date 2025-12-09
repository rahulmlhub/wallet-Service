package com.wallet.service;


import com.wallet.payload.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    // Wallet operations
    WalletDTO createWallet(CreateWalletRequest request);
    WalletDTO getWallet(UUID walletId);
    BigDecimal getBalance(UUID walletId);
    WalletDTO freezeWallet(UUID walletId);
    WalletDTO unfreezeWallet(UUID walletId);

    // Transaction operations
    TransactionDTO deposit(UUID walletId, DepositWithdrawRequest request);
    TransactionDTO withdraw(UUID walletId, DepositWithdrawRequest request);
    TransferResponse transfer(TransferRequest request);
    TransactionDTO reverseTransaction(UUID transactionId);

    // Query operations
    Page<TransactionDTO> getTransactionHistory(UUID walletId, TransactionHistoryRequest request);
}


