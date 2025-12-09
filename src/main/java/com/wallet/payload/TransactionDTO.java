package com.wallet.payload;

import com.wallet.model.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransactionDTO {
    private UUID id;
    private UUID walletId;
    private Transaction.TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceId;
    private String remarks;
    private LocalDateTime timestamp;

    public static TransactionDTO fromEntity(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setWalletId(transaction.getWallet().getId());
        dto.setType(transaction.getType());
        dto.setAmount(transaction.getAmount());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setRemarks(transaction.getRemarks());
        dto.setTimestamp(transaction.getTimestamp());
        return dto;
    }
}
