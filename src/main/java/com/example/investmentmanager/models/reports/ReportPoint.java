package com.example.investmentmanager.models.reports;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportPoint(LocalDate date, BigDecimal accountBalance) {
    /*
    * Data transfer object representing an account balance at a specific point in time
    * Used to construct historical balance reports on time-series charts.
    */
}