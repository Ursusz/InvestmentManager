package com.example.investmentmanager.models.transactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TaxPayment extends Transaction{
    public TaxPayment(BigDecimal amount, LocalDateTime dateTime, String description) {
        super(amount, dateTime, description);
    }

    @Override
    public BigDecimal applyTransaction() {
        return this.amount.negate();
    }
}
