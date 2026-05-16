package com.example.investmentmanager.models.reports.impl;
import com.example.investmentmanager.models.records.TransactionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.investmentmanager.models.reports.IReportGenerable;
import com.example.investmentmanager.models.transactions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AccountStatementGenerator implements IReportGenerable {
    private Collection<Transaction> processedTransactions;
    private BigDecimal finalBalance;

    public String generateStatement(Collection<Transaction> transactions, String type, LocalDate startDate, LocalDate endDate) {
        buildData(transactions.stream()
                .filter(tx -> tx.getDateTime().isAfter(startDate.atStartOfDay()) && tx.getDateTime().isBefore(endDate.atStartOfDay()))
                .filter(tx -> matchesType(tx, type))
                .toList());
        return generateReport();
    }

    private boolean matchesType(Transaction tx, String type){
        if(type.equals("X")) return true;
        return getTransactionType(tx).equals(type);
    }

    private String getTransactionType(Transaction tx){
        return switch(tx){
            case Deposit d -> "Deposit";
            case Withdrawal w -> "Withdrawal";
            case InterestPayment i -> "Interest";
            case TaxPayment t -> "Tax";
            default -> "Unknown";
        };
    }

    @Override
    public void buildData(Collection<Transaction> transactions) {
        if(transactions == null || transactions.isEmpty()){
            //System.out.println("There are no transactions available");
            return;
        }else{
            this.processedTransactions = transactions;
            this.finalBalance = transactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    @Override
    public String generateReport() {
        if(processedTransactions == null || processedTransactions.isEmpty()){
            //System.out.println("No transactions found");
            return "There are no transactions available";
        }

        long index = 0;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        List<TransactionResponse> jsonList = new ArrayList<>();

        for(var tx : processedTransactions){
            //System.out.println(++index + ". Tip: " + getTransactionType(tx) + " | Suma: " + tx.getAmount() +  " | Detalii: " + tx.getDetails() + " | Data : " + tx.getDateTime().format(dtf));

            jsonList.add(new TransactionResponse(
                    ++index,
                    getTransactionType(tx),
                    tx.getAmount(),
                    tx.getDetails(),
                    tx.getDateTime().format(dtf)
            ));
        }

        ObjectMapper mapper = new ObjectMapper();

        try{
            String finalJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonList);

            //System.out.println(finalJson);
            return finalJson;
        }catch(JsonProcessingException e){
            //System.out.println(e.getMessage());
            return "Error" + e.getMessage();
        }
    }
}
