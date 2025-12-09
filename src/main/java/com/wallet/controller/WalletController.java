package com.wallet.controller;

import com.wallet.payload.*;
import com.wallet.service.WalletService;
import com.wallet.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller responsible for managing wallet lifecycle operations,
 * transactions, transfers, and wallet status updates.
 * Exposes all wallet-related APIs under "/api/wallets".
 */
@Tag(name = "Wallet Management", description = "Wallet System APIs")
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    /**
     * Create a new wallet for a user.
     */
    @Operation(summary = "Create a new wallet")
    @PostMapping
    public ResponseEntity<WalletDTO> createWallet(
            @Valid @RequestBody CreateWalletRequest request) {

        WalletDTO wallet = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    /**
     * Retrieve wallet details using its unique wallet ID.
     */
    @Operation(summary = "Get wallet details by ID")
    @GetMapping("/{walletId}")
    public ResponseEntity<WalletDTO> getWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId) {

        WalletDTO wallet = walletService.getWallet(walletId);
        return ResponseEntity.ok(wallet);
    }

    /**
     * Deposit specified funds into a wallet.
     */
    @Operation(summary = "Deposit funds to wallet")
    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<TransactionDTO> deposit(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Valid @RequestBody DepositWithdrawRequest request) {

        TransactionDTO transaction = walletService.deposit(walletId, request);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Withdraw funds from a wallet.
     */
    @Operation(summary = "Withdraw funds from wallet")
    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Valid @RequestBody DepositWithdrawRequest request) {

        TransactionDTO transaction = walletService.withdraw(walletId, request);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Transfer funds between two wallets.
     */
    @Operation(summary = "Transfer funds between wallets")
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {

        TransferResponse response = walletService.transfer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve paginated transaction history for a specific wallet.
     */
    @Operation(summary = "Get transaction history with pagination")
    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<Page<TransactionDTO>> getTransactions(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @ModelAttribute TransactionHistoryRequest request) {

        Page<TransactionDTO> transactions = walletService.getTransactionHistory(walletId, request);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Fetch the current balance of a wallet.
     */
    @Operation(summary = "Get wallet balance")
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId) {

        BigDecimal balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Freeze a wallet, preventing all transactions temporarily.
     */
    @Operation(summary = "Freeze wallet (prevent transactions)")
    @PostMapping("/{walletId}/freeze")
    public ResponseEntity<WalletDTO> freezeWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId) {

        WalletDTO wallet = walletService.freezeWallet(walletId);
        return ResponseEntity.ok(wallet);
    }

    /**
     * Unfreeze a wallet and allow transactions again.
     */
    @Operation(summary = "Unfreeze wallet (allow transactions)")
    @PostMapping("/{walletId}/unfreeze")
    public ResponseEntity<WalletDTO> unfreezeWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId) {

        WalletDTO wallet = walletService.unfreezeWallet(walletId);
        return ResponseEntity.ok(wallet);
    }

    /**
     * Reverse a previously executed transaction (idempotent reversal).
     */
    @Operation(summary = "Reverse a transaction")
    @PostMapping("/transactions/{transactionId}/reverse")
    public ResponseEntity<TransactionDTO> reverseTransaction(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {

        TransactionDTO transaction = walletService.reverseTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Retrieve details of a specific transaction.
     */
    @Operation(summary = "Get specific transaction details")
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransaction(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {

        TransactionDTO transaction = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }
}
