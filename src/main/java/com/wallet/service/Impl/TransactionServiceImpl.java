package com.wallet.service.Impl;

import com.wallet.payload.TransactionDTO;
import com.wallet.exception.WalletException;
import com.wallet.model.Transaction;
import com.wallet.repository.TransactionRepository;
import com.wallet.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service implementation responsible for exposing read operations related to
 * wallet transactions. This layer ensures that clients receive DTOs rather
 * than entities, and also enforces error handling rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Fetch a single transaction by its unique identifier.
     *
     * @param transactionId Unique transaction UUID
     * @return TransactionDTO mapped from entity
     * @throws WalletException if no transaction is found
     */
    @Override
    public TransactionDTO getTransaction(UUID transactionId) {

        // Fetch the transaction or throw a domain-specific exception
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new WalletException("Transaction not found: " + transactionId));

        // Convert entity → DTO to avoid exposing persistence-layer details
        return TransactionDTO.fromEntity(transaction);
    }

    /**
     * Returns paginated transactions for a specific wallet, sorted by timestamp (latest first).
     *
     * @param walletId Wallet identifier
     * @param page     Page number (0-indexed)
     * @param size     Page size
     * @return Paginated list of transactions as DTOs
     */
    @Override
    public Page<TransactionDTO> getTransactionsByWallet(UUID walletId, int page, int size) {

        // Build pageable with descending sort to ensure most recent records appear first
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        // Query repository using paging
        Page<Transaction> transactions = transactionRepository.findByWalletId(walletId, pageable);

        // Transform all entities → DTOs efficiently using map() from Spring Data Page
        return transactions.map(TransactionDTO::fromEntity);
    }

    /**
     * Returns paginated transactions for a wallet filtered by transaction type.
     *
     * @param walletId Wallet identifier
     * @param type     Transaction type (DEPOSIT, WITHDRAW, etc.)
     * @param page     Page number
     * @param size     Page size
     * @return Paginated, filtered list of TransactionDTO
     */
    @Override
    public Page<TransactionDTO> getTransactionsByWalletAndType(UUID walletId, Transaction.TransactionType type, int page, int size) {

        // Build pageable with timestamp sorting for consistency
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        // Run filtered query
        Page<Transaction> transactions = transactionRepository.findByWalletIdAndType(walletId, type, pageable);

        // Convert page content to DTOs
        return transactions.map(TransactionDTO::fromEntity);
    }
}
