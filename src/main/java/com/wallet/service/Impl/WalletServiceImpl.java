package com.wallet.service.Impl;

import com.wallet.exception.*;
import com.wallet.model.Transaction;
import com.wallet.model.Wallet;
import com.wallet.payload.*;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.service.IdempotencyService;
import com.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;

    // -------------------------------------------------------------
    // WALLET CREATION
    // -------------------------------------------------------------

    /**
     * Creates a new wallet for a given user.
     * Ensures:
     *  - User must not already have a wallet
     *  - Currency must be valid
     */
    @Override
    @Transactional
    public WalletDTO createWallet(CreateWalletRequest request) {
        validateCreateWalletRequest(request);

        if (walletRepository.existsByUserId(request.getUserId())) {
            throw new WalletException("Wallet already exists for user: " + request.getUserId());
        }

        Wallet wallet = Wallet.builder()
                .userId(request.getUserId())
                .currency(request.getCurrency())
                .balance(BigDecimal.ZERO)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

       Wallet wallets = walletRepository.saveAndFlush(wallet);
        log.info("Created wallet {} for user {}", wallets.getId(), wallets.getUserId());

        return WalletDTO.fromEntity(wallets);
    }

    // -------------------------------------------------------------
    // READ OPERATIONS
    // -------------------------------------------------------------

    /** Fetch wallet details if wallet exists and is ACTIVE */
    @Override
    public WalletDTO getWallet(UUID walletId) {
        Wallet wallet = findActiveWallet(walletId);
        return WalletDTO.fromEntity(wallet);
    }

    /** Returns balance for an ACTIVE wallet */
    @Override
    public BigDecimal getBalance(UUID walletId) {
        return findActiveWallet(walletId).getBalance();
    }

    // -------------------------------------------------------------
    // FREEZE / UNFREEZE WALLET
    // -------------------------------------------------------------

    /** Freeze wallet to prevent deposits/withdrawals */
    @Override
    @Transactional
    public WalletDTO freezeWallet(UUID walletId) {
        Wallet wallet = findActiveWallet(walletId);
        wallet.setStatus(Wallet.WalletStatus.FROZEN);
        walletRepository.saveAndFlush(wallet);

        log.info("Frozen wallet: {}", walletId);
        return WalletDTO.fromEntity(wallet);
    }

    /** Unfreeze wallet so operations can continue */
    @Override
    @Transactional
    public WalletDTO unfreezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        if (wallet.getStatus() != Wallet.WalletStatus.FROZEN) {
            throw new WalletException("Wallet is not frozen. Current: " + wallet.getStatus());
        }

        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        walletRepository.saveAndFlush(wallet);

        log.info("Unfrozen wallet: {}", walletId);
        return WalletDTO.fromEntity(wallet);
    }

    // -------------------------------------------------------------
    // DEPOSIT
    // -------------------------------------------------------------

    /**
     * Performs a deposit into a wallet.
     * Includes:
     *  - Validation
     *  - Idempotency check (if refId is provided)
     *  - Pessimistic locking via findByIdForUpdate()
     */
    @Override
    @Transactional
    public TransactionDTO deposit(UUID walletId, DepositWithdrawRequest request) {
        validateAmount(request.getAmount());

        if (request.getReferenceId() != null) {
            idempotencyService.checkDuplicate(request.getReferenceId());
        }

        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        validateWalletStatus(wallet);

        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        walletRepository.saveAndFlush(wallet);

        Transaction transaction = createTransaction(
                wallet,
                request.getAmount(),
                Transaction.TransactionType.DEPOSIT,
                request.getReferenceId(),
                request.getRemarks()
        );
        log.info("Deposited {} {} to wallet {}, new balance {}",
                request.getAmount(), wallet.getCurrency(), walletId, newBalance);
        System.out.println("time time "+ transaction.getTimestamp());
        return TransactionDTO.fromEntity(transaction);
    }

    // -------------------------------------------------------------
    // WITHDRAW
    // -------------------------------------------------------------

    /**
     * Withdrawal from wallet.
     * Includes:
     *  - Balance check
     *  - Lock row before modifying
     *  - Idempotency check
     */
    @Override
    @Transactional
    public TransactionDTO withdraw(UUID walletId, DepositWithdrawRequest request) {
        validateAmount(request.getAmount());

        if (request.getReferenceId() != null) {
            idempotencyService.checkDuplicate(request.getReferenceId());
        }

        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        validateWalletStatus(wallet);
        validateSufficientBalance(wallet, request.getAmount());

        BigDecimal newBalance = wallet.getBalance().subtract(request.getAmount());
        wallet.setBalance(newBalance);
        walletRepository.saveAndFlush(wallet);

        Transaction transaction = createTransaction(
                wallet,
                request.getAmount(),
                Transaction.TransactionType.WITHDRAWAL,
                request.getReferenceId(),
                request.getRemarks()
        );

        log.info("Withdrew {} {} from wallet {}, new balance {}",
                request.getAmount(), wallet.getCurrency(), walletId, newBalance);

        return TransactionDTO.fromEntity(transaction);
    }

    // -------------------------------------------------------------
    // TRANSFER
    // -------------------------------------------------------------

    /**
     * Transfer amount between two wallets.
     * Implements:
     *  - Sorted locking to prevent deadlocks
     *  - Balance and status validation
     *  - Currency validation
     *  - Dual linked transaction records
     */
    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        validateAmount(request.getAmount());

        if (request.getFromWalletId().equals(request.getToWalletId())) {
            throw new WalletException("Cannot transfer to the same wallet");
        }

        // lock wallets in sorted order to prevent deadlock
        UUID w1 = request.getFromWalletId();
        UUID w2 = request.getToWalletId();

        List<UUID> ordered = Stream.of(w1, w2)
                .sorted()
                .collect(Collectors.toList());

        List<Wallet> wallets = (List<Wallet>) walletRepository.findAllByIdForUpdate(ordered);

        Wallet fromWallet = findWalletInList(wallets, w1);
        Wallet toWallet = findWalletInList(wallets, w2);

        validateWalletStatus(fromWallet);
        validateWalletStatus(toWallet);

        if (!fromWallet.getCurrency().equals(toWallet.getCurrency())) {
            throw new WalletException("Currency mismatch between wallets");
        }

        validateSufficientBalance(fromWallet, request.getAmount());

        // Adjust balances
        fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));
        toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));
        walletRepository.saveAllAndFlush(List.of(fromWallet, toWallet));

        String remarks = request.getRemarks() != null ? request.getRemarks() : "";

        // Outgoing transaction
        Transaction outTx = createTransaction(
                fromWallet,
                request.getAmount(),
                Transaction.TransactionType.TRANSFER_OUT,
                generateRefId(),
                remarks + " [To: " + toWallet.getId() + "]"
        );

        // Incoming transaction
        Transaction inTx = createTransaction(
                toWallet,
                request.getAmount(),
                Transaction.TransactionType.TRANSFER_IN,
                generateRefId(),
                remarks + " [From: " + fromWallet.getId() + "]"
        );

        // link transactions (bidirectional)
        outTx.setRelatedTransaction(inTx);
        inTx.setRelatedTransaction(outTx);
        transactionRepository.saveAllAndFlush(List.of(outTx, inTx));

        log.info("Transferred {} from {} → {}", request.getAmount(), w1, w2);

        return new TransferResponse(
                TransactionDTO.fromEntity(outTx),
                TransactionDTO.fromEntity(inTx)
        );
    }

    // -------------------------------------------------------------
    // TRANSACTION REVERSAL
    // -------------------------------------------------------------

    /**
     * Reverse a DEPOSIT or WITHDRAWAL.
     * Not allowed for transfer-related transactions.
     */
    @Override
    @Transactional
    public TransactionDTO reverseTransaction(UUID transactionId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new WalletException("Transaction not found: " + transactionId));

        if (original.getType() != Transaction.TransactionType.DEPOSIT &&
                original.getType() != Transaction.TransactionType.WITHDRAWAL) {
            throw new WalletException("Only DEPOSIT/WITHDRAWAL can be reversed");
        }

        // prevent repeated reversals
        if (transactionRepository.existsByReferenceId("REV_" + transactionId)) {
            throw new DuplicateTransactionException("Transaction already reversed");
        }

        Wallet wallet = walletRepository.findByIdForUpdate(original.getWallet().getId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        validateWalletStatus(wallet);

        BigDecimal amount = original.getAmount();
        BigDecimal newBalance;
        Transaction.TransactionType reverseType;

        if (original.getType() == Transaction.TransactionType.DEPOSIT) {
            validateSufficientBalance(wallet, amount);
            newBalance = wallet.getBalance().subtract(amount);
            reverseType = Transaction.TransactionType.WITHDRAWAL;
        } else {
            newBalance = wallet.getBalance().add(amount);
            reverseType = Transaction.TransactionType.DEPOSIT;
        }

        wallet.setBalance(newBalance);
        walletRepository.saveAndFlush(wallet);

        Transaction reversal = createTransaction(
                wallet,
                amount,
                reverseType,
                "REV_" + transactionId,
                "Reversal of: " + transactionId
        );

        log.info("Reversed transaction {}", transactionId);
        return TransactionDTO.fromEntity(reversal);
    }

    // -------------------------------------------------------------
    // TRANSACTION HISTORY
    // -------------------------------------------------------------

    /** Fetch paginated and optionally filtered transaction history */
    @Override
    public Page<TransactionDTO> getTransactionHistory(UUID walletId, TransactionHistoryRequest request) {
        findActiveWallet(walletId);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<Transaction> txPage;

        if (request.getType() != null && !request.getType().isEmpty()) {
            try {
                Transaction.TransactionType type = Transaction.TransactionType.valueOf(request.getType());
                txPage = transactionRepository.findByWalletIdAndType(walletId, type, pageable);
            } catch (IllegalArgumentException e) {
                throw new WalletException("Invalid transaction type: " + request.getType());
            }
        } else {
            txPage = transactionRepository.findByWalletId(walletId, pageable);
        }

        return txPage.map(TransactionDTO::fromEntity);
    }

    // -------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------

    private Wallet findActiveWallet(UUID walletId) {
        return walletRepository.findByIdAndStatus(walletId, Wallet.WalletStatus.ACTIVE)
                .orElseThrow(() -> new WalletNotFoundException("Active wallet not found: " + walletId));
    }

    private Wallet findWalletInList(List<Wallet> wallets, UUID id) {
        return wallets.stream()
                .filter(w -> w.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found in locked set: " + id));
    }

    private void validateCreateWalletRequest(CreateWalletRequest request) {
        if (request.getUserId() == null) {
            throw new WalletException("User ID is required");
        }
        if (request.getCurrency() == null || request.getCurrency().length() != 3) {
            throw new WalletException("Currency must be a 3-letter ISO code");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Amount must be greater than zero");
        }
    }

    private void validateWalletStatus(Wallet wallet) {
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Wallet is " + wallet.getStatus());
        }
    }

    private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
    }

    private Transaction createTransaction(Wallet wallet, BigDecimal amount,
                                          Transaction.TransactionType type,
                                          String referenceId, String remarks) {
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .referenceId(referenceId)
                .remarks(remarks)
                .build();

        return transactionRepository.saveAndFlush(tx);
    }

    private String generateRefId() {
        return UUID.randomUUID().toString();
    }
}
