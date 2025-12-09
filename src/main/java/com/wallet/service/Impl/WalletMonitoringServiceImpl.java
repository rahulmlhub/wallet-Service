package com.wallet.service.Impl;

import com.wallet.payload.WalletDTO;
import com.wallet.model.Wallet;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.service.WalletMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Monitoring and analytics service for wallets.
 * Provides aggregated views such as total system balance,
 * wallet count by status, and all wallets belonging to a user.
 */
@Service
@RequiredArgsConstructor
class WalletMonitoringServiceImpl implements WalletMonitoringService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Fetch all wallets belonging to a specific user.
     *
     * @param userId User identifier
     * @return List of WalletDTOs mapped from entities
     */
    @Override
    public List<WalletDTO> getWalletsByUser(UUID userId) {

        // Repository fetch → convert to DTOs
        List<Wallet> wallets = walletRepository.findByUserId(userId);

        return wallets.stream()
                .map(WalletDTO::fromEntity)
                .toList();
    }

    /**
     * Fetch wallets based on their operational status (e.g., ACTIVE, SUSPENDED).
     *
     * @param status Wallet status
     * @return List of WalletDTOs
     */
    @Override
    public List<WalletDTO> getWalletsByStatus(Wallet.WalletStatus status) {

        // Filter wallets by status → convert to DTOs
        List<Wallet> wallets = walletRepository.findByStatus(status);

        return wallets.stream()
                .map(WalletDTO::fromEntity)
                .toList();
    }

    /**
     * Computes the total balance across all wallets in the system.
     * Useful for system-wide financial monitoring or audits.
     *
     * @return BigDecimal representing the total system-wide wallet balance
     */
    @Override
    public BigDecimal getTotalSystemBalance() {

        // Summation of all wallet balances
        return walletRepository.findAll().stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Counts the number of active wallets in the system.
     *
     * @return Count of wallets with ACTIVE status
     */
    @Override
    public int getActiveWalletCount() {

        // Repository returns a list → we return the size
        return walletRepository.findByStatus(Wallet.WalletStatus.ACTIVE).size();
    }

    /**
     * Retrieves the number of transactions for a specific wallet.
     * Uses an unpaged query to avoid unnecessary pagination for counting.
     *
     * @param walletId Wallet identifier
     * @return total number of transactions for the wallet
     */
    @Override
    public int getTransactionCount(UUID walletId) {

        // Pageable.unpaged() returns all records → get element count
        return transactionRepository
                .findByWalletId(walletId, Pageable.unpaged())
                .getNumberOfElements();
    }
}
