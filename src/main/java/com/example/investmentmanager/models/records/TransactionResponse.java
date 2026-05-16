package com.example.investmentmanager.models.records;

import java.math.BigDecimal;

public record TransactionResponse(
   long id,
   String type,
   BigDecimal amount,
   String details,
   String date
) {}
