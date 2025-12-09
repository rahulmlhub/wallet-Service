package com.wallet.service;


import com.wallet.payload.TransactionDTO;
import com.wallet.model.Transaction;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface TransactionService {
    TransactionDTO getTransaction(UUID transactionId);
    Page<TransactionDTO> getTransactionsByWallet(UUID walletId, int page, int size);
    Page<TransactionDTO> getTransactionsByWalletAndType(UUID walletId, Transaction.TransactionType type, int page, int size);
}
