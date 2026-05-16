package com.example.investmentmanager.models.reports.impl;

import com.example.investmentmanager.models.reports.IReportGenerable;
import com.example.investmentmanager.models.reports.ReportPoint;
import com.example.investmentmanager.models.transactions.Deposit;
import com.example.investmentmanager.models.transactions.Transaction;
import com.example.investmentmanager.models.transactions.Withdrawal;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MonthlyBalanceReportGenerator implements IReportGenerable {
    ArrayList<ReportPoint>  reportPoints = new ArrayList<ReportPoint>();
    BigDecimal totalInvestment = BigDecimal.ZERO;

    @Override
    public void buildData(Collection<Transaction> transactions) {
        reportPoints.clear();
        totalInvestment = BigDecimal.ZERO;

        if(transactions == null || transactions.isEmpty()){
            return;
        }

        //get the last transaction of each month
        /*
        * Collectors.toMap(keyMapper, valueMapper, mergeFunction)
        * keyMapper – a mapping function to produce keys
        * valueMapper – a mapping function to produce values
        * mergeFunction – a merge function, used to resolve collisions between values associated with the same key, as
        */

        for(var t : transactions){
            if(t instanceof Deposit){
                totalInvestment = totalInvestment.add(t.getAmount());
            }
        }

        Map<YearMonth, BigDecimal> monthlyBalances = new LinkedHashMap<>();
        BigDecimal runningBalance = BigDecimal.ZERO;

        for(var t : transactions.stream().sorted().toList()){
            runningBalance = runningBalance.add(t.applyTransaction());
            monthlyBalances.put(YearMonth.from(t.getDateTime()), runningBalance);
        }

        for(var entry : monthlyBalances.entrySet()){
            reportPoints.add(new ReportPoint(entry.getKey().atEndOfMonth(), entry.getValue()));
        }

        reportPoints.sort(Comparator.comparing(ReportPoint::date));
    }

    public static void clearFolder(String folderPath) {
        Path path = Paths.get(folderPath);

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(path))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            System.err.println("Could not delete: " + p + " -> " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error truncating reports dir: " + e.getMessage());
        }
    }

    /*@Override
    public String generateReport() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String folder = "GeneratedReports";
        clearFolder(folder);

        String fileName = folder + "/reports_" + timestamp + ".txt";

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))){
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            long index = 0;
            for(var r : reportPoints){
                bw.write(String.valueOf(++index));
                bw.write("\t\t");
                bw.write(r.date().format(dtf));
                bw.write("\t");
                bw.write(String.valueOf(r.accountBalance().setScale(2, RoundingMode.HALF_UP).doubleValue()));
                bw.newLine();
            }

            BigDecimal currentBalance = reportPoints.getFirst().accountBalance().setScale(2, RoundingMode.HALF_UP);
            BigDecimal netProfit = currentBalance.subtract(totalInvestment).setScale(2, RoundingMode.HALF_UP);

            BigDecimal profitPercentage;

            if (totalInvestment.compareTo(BigDecimal.ZERO) != 0) {
                profitPercentage = netProfit
                        .divide(totalInvestment, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                profitPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            bw.write("TOTAL INVESTIT: ");
            bw.write(String.valueOf(totalInvestment));
            bw.write("\t|\tFINAL SOLD : ");
            bw.write(String.valueOf(currentBalance));
            bw.write("\t|\tPROFIT NET : ");
            bw.write(String.valueOf(netProfit));
            bw.write("\t|\tPROCENT PROFIT : ");
            bw.write(String.valueOf(profitPercentage.setScale(2, RoundingMode.HALF_UP).doubleValue()));
            bw.write(" %");
        }catch(IOException e){
            System.out.println("Error while writing reports file" + e.getMessage());
        }
        return "todo";
    }*/

    @Override
    public String generateReport() {
        if (reportPoints == null || reportPoints.isEmpty()) {
            return """
                {
                  "points": [],
                  "totalInvestment": 0.00,
                  "currentBalance": 0.00,
                  "netProfit": 0.00,
                  "profitPercentage": 0.00
                }
                """;
        }

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM-yyyy");

        var sortedPoints = reportPoints.stream()
                .sorted(Comparator.comparing(ReportPoint::date))
                .toList();

        BigDecimal currentBalance = sortedPoints.getLast()
                .accountBalance()
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netProfit = currentBalance
                .subtract(totalInvestment)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal profitPercentage;

        if (totalInvestment.compareTo(BigDecimal.ZERO) != 0) {
            profitPercentage = netProfit
                    .divide(totalInvestment, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            profitPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"points\":[");

        for (int i = 0; i < sortedPoints.size(); i++) {
            ReportPoint point = sortedPoints.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{");
            json.append("\"date\":\"").append(point.date()).append("\",");
            json.append("\"month\":\"").append(point.date().format(monthFormatter)).append("\",");
            json.append("\"accountBalance\":")
                    .append(point.accountBalance().setScale(2, RoundingMode.HALF_UP));
            json.append("}");
        }

        json.append("],");
        json.append("\"totalInvestment\":").append(totalInvestment.setScale(2, RoundingMode.HALF_UP)).append(",");
        json.append("\"currentBalance\":").append(currentBalance).append(",");
        json.append("\"netProfit\":").append(netProfit).append(",");
        json.append("\"profitPercentage\":").append(profitPercentage);
        json.append("}");

        return json.toString();
    }
}
