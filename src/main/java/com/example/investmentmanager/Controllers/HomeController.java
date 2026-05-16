package com.example.investmentmanager.Controllers;

import com.example.investmentmanager.models.enums.DepositResponse;
import com.example.investmentmanager.models.enums.WithdrawalResponse;
import com.example.investmentmanager.service.impl.InvestmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class HomeController{
    private final InvestmentService service;

    public HomeController(InvestmentService service){
        this.service = service;
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestParam double amount, @RequestParam String description, @RequestParam int day,
                        @RequestParam int month, @RequestParam int year){
        DepositResponse res = service.deposit(amount, LocalDate.of(year, month, day).atStartOfDay(), description);

       return switch(res){
           case SUCCESS -> ResponseEntity.ok("Deposited Successfully");
           case MONTH_PROCESSED ->  ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot deposit. Month is processed.");
           default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown Error");
       };
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestParam double amount, @RequestParam String description){
        WithdrawalResponse res = service.withdraw(amount, description);

        return switch(res){
            case SUCCESS -> ResponseEntity.ok("Withdrawal accepted");
            case INSUFFICIENT_FUNDS -> ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient Funds");
            case MONTH_PROCESSED -> ResponseEntity.status(HttpStatus.FORBIDDEN).body("Month Processed");
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown Error");
        };
    }

    @GetMapping("/generateAccountSatement")
    public String generateAccountSatement(@RequestParam(required = false) String filter,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){
        String f = (filter != null) ? filter : "X";
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusDays(1);

        return service.generateAccountStatement(f,  start, end);
    }

    @PostMapping("/backFillMonthlyInterest")
    public void backFillMonthlyInterest(){
        service.backfillMonthlyInterest();
    }

    @GetMapping("/generateMonthlyReport")
    public String generateMonthlyReport(){
        return service.generateMonthlyBalanceReport();
    }

    @GetMapping("/resetAccount")
    public void reset(){
        service.resetAccount();
    }
}
