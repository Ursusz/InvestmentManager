package com.example.investmentmanager.models.reports;

import com.example.investmentmanager.models.transactions.Transaction;

import java.util.Collection;

public interface IReportGenerable {
    public void buildData(Collection<Transaction> transactions);
    public String generateReport();
}
