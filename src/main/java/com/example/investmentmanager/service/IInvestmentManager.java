package com.example.investmentmanager.service;

import com.example.investmentmanager.models.enums.DepositResponse;
import com.example.investmentmanager.models.enums.WithdrawalResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface IInvestmentManager {
    DepositResponse deposit(double amount, String description);
    DepositResponse deposit(double amount, LocalDateTime time, String description);
    WithdrawalResponse withdraw(double amount, String description);
    void processMonthlyInterest();
    void processMonthlyInterest(LocalDate month);
    String generateAccountStatement(String filter, LocalDate startDate, LocalDate endDate);
    void printBalance();
    String generateMonthlyBalanceReport();
    void backfillMonthlyInterest();
}