package com.example.investmentmanager.models;

import com.example.investmentmanager.exceptions.InvalidMonth;
import com.example.investmentmanager.models.transactions.InterestPayment;
import com.example.investmentmanager.models.transactions.TaxPayment;
import com.example.investmentmanager.models.transactions.Transaction;
import jakarta.transaction.TransactionScoped;
import org.jspecify.annotations.NonNull;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;

public class SavingsAccount {
    private BigDecimal balance;
    private final BigDecimal interestRate;

    TreeSet<Transaction> transactions = new TreeSet<Transaction>();
    InterestBuffer interestBuffer = new InterestBuffer();

    public SavingsAccount(){
        UUID accountID = UUID.randomUUID();
        this.balance = BigDecimal.ZERO;
        this.interestRate = BigDecimal.valueOf(0.035);
    }

    public void processTransaction(Transaction tx){
        tx.setBalanceBeforeTransaction(this.balance);
        transactions.add(tx);
        balance = balance.add(tx.applyTransaction());
    }

    public BigDecimal getBalanceAtStartOfMonth(LocalDate month) {
        LocalDate firstOfMonth = month.withDayOfMonth(1);

        return transactions.stream()
                .filter(tx -> tx.getDateTime().toLocalDate().isBefore(firstOfMonth))
                .map(Transaction::applyTransaction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private @NonNull String getMonthName(int monthNumber){
        return switch(monthNumber){
            case 1 -> "ianuarie";
            case 2 -> "februarie";
            case 3 -> "martie";
            case 4 -> "aprilie";
            case 5 -> "mai";
            case 6 -> "iunie";
            case 7 -> "iulie";
            case 8 -> "august";
            case 9 -> "septembrie";
            case 10 -> "octombrie";
            case 11 -> "noiembrie";
            case 12 -> "decembrie";
            default -> throw new InvalidMonth("Unexpected value: " + monthNumber);
        };
    }

    public void processMonthlyInterest(LocalDate month) {
        interestBuffer.accumulate(transactions, interestRate, getBalanceAtStartOfMonth(month), month);
        final BigDecimal taxAmount = interestBuffer.calculateTax().setScale(2, RoundingMode.DOWN);
        final BigDecimal flushAmount = interestBuffer.flush().setScale(2, RoundingMode.DOWN);

        // we have to manually set the timings to ensure they are inserted correctly in the treeSet of transactions (comparable on time)
        LocalDateTime timeInterest = month.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 58);
        LocalDateTime timeTax = month.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        try{
            String monthName = getMonthName(month.getMonthValue());

            processTransaction(new InterestPayment(flushAmount, timeInterest, "Plata dobanda lunara aferenta lunii " + monthName));
            processTransaction(new TaxPayment(taxAmount, timeTax, "Plata impozit lunar aferent lunii " + monthName));
        }catch(InvalidMonth e){
            processTransaction(new InterestPayment(flushAmount, timeInterest, "Plata dobanda lunara. Eroare detectare luna tranzactie"));
            processTransaction(new TaxPayment(taxAmount, timeTax, "Plata impozit lunar aferent dobanda. Eroare detectare luna tranzactie"));
        }
    }

    private boolean wasInterestProcessed(LocalDate dateInMonth) {
        return transactions.stream().anyMatch(tx ->
                tx instanceof InterestPayment &&
                        tx.getDateTime().getMonth() == dateInMonth.getMonth() &&
                        tx.getDateTime().getYear() == dateInMonth.getYear()
        );
    }

    private boolean wasTaxProcessed(LocalDate dateInMonth) {
        return transactions.stream().anyMatch(tx ->
                tx instanceof TaxPayment &&
                        tx.getDateTime().getMonth() == dateInMonth.getMonth() &&
                        tx.getDateTime().getYear() == dateInMonth.getYear()
        );
    }

    public void backfillMonthlyInterest(){
        if(transactions == null || transactions.isEmpty()){
            return;
        }

        LocalDate start = transactions.getFirst().getDateTime().toLocalDate().withDayOfMonth(1);
        LocalDate end = transactions.getLast().getDateTime().toLocalDate().withDayOfMonth(1);

        LocalDate today = LocalDate.now();
        LocalDate currentMonth = today.withDayOfMonth(1);
        boolean isLastDayOfCurrentMonth = today.equals(today.with(TemporalAdjusters.lastDayOfMonth()));

        for(LocalDate date = start; !date.isAfter(end); date = date.plusMonths(1)){
            boolean isPastMonth = date.isBefore(currentMonth);
            boolean isCurrentMonthAndLastDay = date.equals(currentMonth) && isLastDayOfCurrentMonth;

            if((isPastMonth || isCurrentMonthAndLastDay) && !wasInterestProcessed(date) && !wasTaxProcessed(date)){
                processMonthlyInterest(date.with(TemporalAdjusters.lastDayOfMonth()));
            }
        }
    }

    public void printBalance(){
        System.out.println("Account balance as of " + LocalDate.now() + " : " + this.balance.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    public Collection<Transaction> getTransactions() {
        return this.transactions;
    }

    public void reset(){
        transactions.clear();
        balance = BigDecimal.ZERO;
    }

    public Boolean isMonthProcessed(LocalDate date){
        return wasInterestProcessed(date) || wasTaxProcessed(date);
    }

    public Boolean haveSufficientFunds(double amount){
        if(this.balance.subtract(BigDecimal.valueOf(amount)).compareTo(BigDecimal.ZERO) < 0){
            return false;
        }
        return true;
    }
}
