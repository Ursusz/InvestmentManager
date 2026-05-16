package com.example.investmentmanager.models.transactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InterestPayment extends Transaction {
    public InterestPayment(BigDecimal amount, LocalDateTime dateTime, String description) {
        super(amount, dateTime, description);
    }

    @Override
    public BigDecimal applyTransaction() {
        return this.amount;
    }
}
