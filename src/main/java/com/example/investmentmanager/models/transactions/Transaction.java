package com.example.investmentmanager.models.transactions;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Transaction implements Comparable<Transaction> {
    protected UUID id;
    @Getter
    protected BigDecimal amount;
    @Setter
    protected BigDecimal balanceBeforeTransaction;
    @Getter
    protected LocalDateTime dateTime;
    protected String description;

    protected Transaction(BigDecimal amount, LocalDateTime dateTime, String description) {
        this.id = UUID.randomUUID();
        this.amount = amount;
        this.dateTime = dateTime;
        this.description = description;
    }

    public String getDetails(){
        return this.description;
    }

    @Override
    public int compareTo(Transaction rhs) {
        int dateCompare = this.dateTime.compareTo(rhs.dateTime);
        if (dateCompare != 0) return dateCompare;

        return this.id.compareTo(rhs.id);
    }

    abstract public BigDecimal applyTransaction();

    public BigDecimal getBalanceAfterTransaction(){
        return this.balanceBeforeTransaction.add(applyTransaction());
    }
}
