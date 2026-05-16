package com.example.investmentmanager;

import com.example.investmentmanager.models.SavingsAccount;
import com.example.investmentmanager.service.impl.InvestmentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public SavingsAccount savingsAccount() {
        return new SavingsAccount();
    }

    @Bean
    public InvestmentService investmentService(SavingsAccount savingsAccount) {
        return new InvestmentService(savingsAccount);
    }
}