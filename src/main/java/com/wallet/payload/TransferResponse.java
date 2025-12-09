package com.wallet.payload;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private TransactionDTO fromTransaction;
    private TransactionDTO toTransaction;
    private String message = "Transfer completed successfully";


    public TransferResponse(TransactionDTO fromTransaction, TransactionDTO toTransaction) {
        this.fromTransaction = fromTransaction;
        this.toTransaction = toTransaction;
        this.message = "Transfer completed successfully";
    }
}
