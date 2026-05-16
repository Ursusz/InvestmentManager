package com.example.investmentmanager.models;

import com.example.investmentmanager.models.transactions.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.TreeSet;

public class InterestBuffer {
    private BigDecimal accruedAmount = BigDecimal.ZERO;

    public void accumulate(TreeSet<Transaction> transactions, BigDecimal interestRate, BigDecimal startingBalance, LocalDate month){
        if(transactions == null){
            return;
        }

        BigDecimal currentBalance = startingBalance;
        LocalDate lastDate = month.withDayOfMonth(1);

        LocalDate firstOfMonth = month.withDayOfMonth(1);
        LocalDate lastOfMonth = month.with(TemporalAdjusters.lastDayOfMonth());

        int daysInYear = 360;
        for(var tx: transactions){
            long daysBetween = ChronoUnit.DAYS.between(lastDate, tx.getDateTime().toLocalDate());

            LocalDate txDate = tx.getDateTime().toLocalDate();
            if(txDate.isBefore(firstOfMonth) || txDate.isAfter(lastOfMonth)){
                continue;
            }

            if(daysBetween > 0){
                BigDecimal rate = interestRate.divide(BigDecimal.valueOf(daysInYear), 10, RoundingMode.HALF_UP);
                this.accruedAmount = this.accruedAmount
                        .add(currentBalance
                            .multiply(rate)
                            .multiply(BigDecimal.valueOf(daysBetween)));

            }

            currentBalance = currentBalance.add(tx.applyTransaction());
            lastDate = LocalDate.from(tx.getDateTime());
        }

        LocalDate endOfMonth = month.with(TemporalAdjusters.lastDayOfMonth());
        long remainingDays = ChronoUnit.DAYS.between(lastDate, endOfMonth) + 1;
        if(remainingDays > 0){
            BigDecimal rate = interestRate.divide(BigDecimal.valueOf(daysInYear), 10, RoundingMode.HALF_UP);
            this.accruedAmount = this.accruedAmount
                    .add(currentBalance
                        .multiply(rate)
                        .multiply(BigDecimal.valueOf(remainingDays)));

        }
    }

    public BigDecimal calculateTax(){
        BigDecimal taxRate = BigDecimal.valueOf(0.10);
        return this.accruedAmount.multiply(taxRate);
    }

    public BigDecimal getNetAmount(){
        return this.accruedAmount.subtract(calculateTax());
    }

    public BigDecimal flush(){
        BigDecimal toReturn = this.accruedAmount;
        this.accruedAmount = BigDecimal.ZERO;
        return toReturn;
    }
}
