package com.example.investmentmanager.service.impl;

import com.example.investmentmanager.models.SavingsAccount;
import com.example.investmentmanager.models.enums.DepositResponse;
import com.example.investmentmanager.models.enums.WithdrawalResponse;
import com.example.investmentmanager.models.reports.impl.AccountStatementGenerator;
import com.example.investmentmanager.models.reports.impl.MonthlyBalanceReportGenerator;
import com.example.investmentmanager.models.transactions.Deposit;
import com.example.investmentmanager.models.transactions.Transaction;
import com.example.investmentmanager.models.transactions.Withdrawal;
import com.example.investmentmanager.service.IInvestmentManager;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InvestmentService implements IInvestmentManager {
    private final SavingsAccount savingsAccount;

    public InvestmentService(SavingsAccount savingsAccount) {
        this.savingsAccount = savingsAccount;
    }

    @Override
    public DepositResponse deposit(double amount, String description) {
        if(savingsAccount.isMonthProcessed(LocalDate.now())) {
            return DepositResponse.MONTH_PROCESSED;
        }

        Transaction tx = new Deposit(BigDecimal.valueOf(amount), LocalDateTime.now(), description);
        savingsAccount.processTransaction(tx);

        return DepositResponse.SUCCESS;
    }

    @Override
    public DepositResponse deposit(double amount, LocalDateTime time, String description){
        if(savingsAccount.isMonthProcessed(time.toLocalDate())) {
            return DepositResponse.MONTH_PROCESSED;
        }

        Transaction tx = new Deposit(BigDecimal.valueOf(amount), time, description);
        savingsAccount.processTransaction(tx);

        return DepositResponse.SUCCESS;
    }

    @Override
    public WithdrawalResponse withdraw(double amount, String description) {
        if(!savingsAccount.haveSufficientFunds(amount)){
            return WithdrawalResponse.INSUFFICIENT_FUNDS;
        }
        if(savingsAccount.isMonthProcessed(LocalDate.now())) {
            return WithdrawalResponse.MONTH_PROCESSED;
        }
        Transaction tx = new Withdrawal(BigDecimal.valueOf(amount), LocalDateTime.now(), description);
        savingsAccount.processTransaction(tx);

        return WithdrawalResponse.SUCCESS;
    }

    @Override
    public void processMonthlyInterest(){
        savingsAccount.processMonthlyInterest(LocalDate.now());
    }

    @Override
    public void processMonthlyInterest(LocalDate month) {
        savingsAccount.processMonthlyInterest(month);
    }

    @Override
    public void backfillMonthlyInterest(){
        savingsAccount.backfillMonthlyInterest();
    }

    @Override
    public String generateAccountStatement(String filter, LocalDate startDate, LocalDate endDate) {
        AccountStatementGenerator generator = new AccountStatementGenerator();

        return generator.generateStatement(savingsAccount.getTransactions(), filter, startDate, endDate);
    }

    @Override
    public void printBalance() {
        savingsAccount.printBalance();
    }

    @Override
    public String generateMonthlyBalanceReport() {
        MonthlyBalanceReportGenerator generator = new MonthlyBalanceReportGenerator();

        generator.buildData(savingsAccount.getTransactions());
        return generator.generateReport();
    }

    public void resetAccount(){
        savingsAccount.reset();
    }
}
