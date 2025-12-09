package com.wallet.service;

import com.wallet.exception.DuplicateTransactionException;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.model.Wallet;
import com.wallet.payload.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;


    private UUID walletId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);
        request.setCurrency("USD");

        WalletDTO wallet = walletService.createWallet(request);
        walletId = wallet.getId();

        // Initial deposit (committed because test transactions are NOT_SUPPORTED)
        DepositWithdrawRequest deposit = new DepositWithdrawRequest();
        deposit.setAmount(new BigDecimal("1000.00"));
        walletService.deposit(walletId, deposit);
    }

    @Test
    void testConcurrentDeposits() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    DepositWithdrawRequest deposit = new DepositWithdrawRequest();
                    deposit.setAmount(new BigDecimal("100.00"));
                    // use UUID to make sure each thread generates a truly unique referenceId
                    deposit.setReferenceId("REF_" + threadNum + "_" + UUID.randomUUID());

                    walletService.deposit(walletId, deposit);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Thread " + threadNum + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        BigDecimal balance = walletService.getBalance(walletId);

        // Use compareTo for BigDecimal equality to ignore scale differences
        BigDecimal expectedBalance = new BigDecimal("1000.00")
                .add(new BigDecimal("100.00").multiply(new BigDecimal(successCount.get())));

        assertEquals(0, expectedBalance.compareTo(balance),
                () -> "Expected balance " + expectedBalance + " but was " + balance);
        assertEquals(threadCount, successCount.get());
    }

    @Test
    void testConcurrentWithdrawals() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    DepositWithdrawRequest withdraw = new DepositWithdrawRequest();
                    withdraw.setAmount(new BigDecimal("300.00"));
                    withdraw.setReferenceId("WITHDRAW_" + threadNum + "_" + UUID.randomUUID());

                    walletService.withdraw(walletId, withdraw);
                    successCount.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Thread " + threadNum + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Since total balance is 1000, at most 3 withdrawals of 300 should succeed
        assertEquals(3, successCount.get());
        assertEquals(2, failureCount.get());

        BigDecimal balance = walletService.getBalance(walletId);
        BigDecimal expectedBalance = new BigDecimal("100.00"); // 1000 - (3 * 300) = 100

        assertEquals(0, expectedBalance.compareTo(balance),
                () -> "Expected balance " + expectedBalance + " but was " + balance);
    }

    @Test
    void testTransferBetweenWallets() {
        // Create second wallet
        CreateWalletRequest secondWalletRequest = new CreateWalletRequest();
        secondWalletRequest.setUserId(UUID.randomUUID());
        WalletDTO secondWallet = walletService.createWallet(secondWalletRequest);
        UUID secondWalletId = secondWallet.getId();

        // Deposit to second wallet
        DepositWithdrawRequest deposit = new DepositWithdrawRequest();
        deposit.setAmount(new BigDecimal("500.00"));
        walletService.deposit(secondWalletId, deposit);

        // Perform transfer
        TransferRequest transfer = new TransferRequest();
        transfer.setFromWalletId(walletId);
        transfer.setToWalletId(secondWalletId);
        transfer.setAmount(new BigDecimal("200.00"));
        transfer.setReferenceId("TRANSFER_" + UUID.randomUUID());

        TransferResponse response = walletService.transfer(transfer);

        assertNotNull(response);
        assertNotNull(response.getFromTransaction());
        assertNotNull(response.getToTransaction());

        // Verify balances (use compareTo)
        BigDecimal firstBalance = walletService.getBalance(walletId);
        BigDecimal secondBalance = walletService.getBalance(secondWalletId);

        assertEquals(0, new BigDecimal("800.00").compareTo(firstBalance));
        assertEquals(0, new BigDecimal("700.00").compareTo(secondBalance));
    }

    @Test
    void testTransferWithInsufficientFunds() {
        // Create second wallet
        CreateWalletRequest secondWalletRequest = new CreateWalletRequest();
        secondWalletRequest.setUserId(UUID.randomUUID());
        WalletDTO secondWallet = walletService.createWallet(secondWalletRequest);
        UUID secondWalletId = secondWallet.getId();

        TransferRequest transfer = new TransferRequest();
        transfer.setFromWalletId(walletId);
        transfer.setToWalletId(secondWalletId);
        transfer.setAmount(new BigDecimal("2000.00")); // More than available

        assertThrows(InsufficientFundsException.class, () -> walletService.transfer(transfer));

        // Verify no changes to balances
        BigDecimal firstBalance = walletService.getBalance(walletId);
        BigDecimal secondBalance = walletService.getBalance(secondWalletId);

        assertEquals(0, new BigDecimal("1000.00").compareTo(firstBalance));
        assertEquals(0, BigDecimal.ZERO.compareTo(secondBalance));
    }

    @Test
    void testIdempotency() {
        String referenceId = "UNIQUE_REF_" + UUID.randomUUID();

        DepositWithdrawRequest deposit = new DepositWithdrawRequest();
        deposit.setAmount(new BigDecimal("100.00"));
        deposit.setReferenceId(referenceId);

        // First deposit should succeed
        TransactionDTO firstTransaction = walletService.deposit(walletId, deposit);
        assertNotNull(firstTransaction);

        // Second deposit with same reference should fail (duplicate detection)
        assertThrows(DuplicateTransactionException.class, () -> walletService.deposit(walletId, deposit));

        // Balance should only reflect first deposit
        BigDecimal balance = walletService.getBalance(walletId);
        assertEquals(0, new BigDecimal("1100.00").compareTo(balance));
    }

    @Test
    void testWalletFreezeAndUnfreeze() {
        // Freeze wallet
        WalletDTO frozenWallet = walletService.freezeWallet(walletId);
        assertEquals(Wallet.WalletStatus.FROZEN, frozenWallet.getStatus());

        // Try to deposit to frozen wallet
        DepositWithdrawRequest deposit = new DepositWithdrawRequest();
        deposit.setAmount(new BigDecimal("100.00"));

        assertThrows(RuntimeException.class, () -> walletService.deposit(walletId, deposit)); // replace with specific WalletFrozenException if present

        // Unfreeze wallet
        WalletDTO unfrozenWallet = walletService.unfreezeWallet(walletId);
        assertEquals(Wallet.WalletStatus.ACTIVE, unfrozenWallet.getStatus());

        // Now deposit should work
        TransactionDTO transaction = walletService.deposit(walletId, deposit);
        assertNotNull(transaction);
    }

    @Test
    void testTransactionReversal() {
        // Make a withdrawal
        DepositWithdrawRequest withdraw = new DepositWithdrawRequest();
        withdraw.setAmount(new BigDecimal("200.00"));
        withdraw.setReferenceId("WITHDRAW_TO_REVERSE_" + UUID.randomUUID());

        TransactionDTO withdrawal = walletService.withdraw(walletId, withdraw);
        assertNotNull(withdrawal);

        BigDecimal balanceAfterWithdrawal = walletService.getBalance(walletId);
        assertEquals(0, new BigDecimal("800.00").compareTo(balanceAfterWithdrawal));

        // Reverse the transaction
        TransactionDTO reversal = walletService.reverseTransaction(withdrawal.getId());
        assertNotNull(reversal);

        BigDecimal balanceAfterReversal = walletService.getBalance(walletId);
        assertEquals(0, new BigDecimal("1000.00").compareTo(balanceAfterReversal));

        // Try to reverse again (should fail)
        assertThrows(DuplicateTransactionException.class, () -> walletService.reverseTransaction(withdrawal.getId()));
    }
}
