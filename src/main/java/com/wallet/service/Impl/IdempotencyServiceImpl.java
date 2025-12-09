package com.wallet.service.Impl;


import com.wallet.exception.DuplicateTransactionException;
import com.wallet.repository.TransactionRepository;
import com.wallet.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkDuplicate(String referenceId) {
        if (referenceId == null || referenceId.trim().isEmpty()) {
            return;
        }

        if (transactionRepository.existsByReferenceId(referenceId)) {
            log.warn("Duplicate transaction detected with referenceId: {}", referenceId);
            throw new DuplicateTransactionException(
                    "Duplicate transaction detected with referenceId: " + referenceId);
        }

        log.debug("Reference ID {} is unique, proceeding with transaction", referenceId);
    }
}
