package com.wallet.service;

public interface IdempotencyService {
     void checkDuplicate(String referenceId);
}
