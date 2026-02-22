package com.suarez.expenses.account;

import com.suarez.expenses.transaction.BudgetTransaction;
import com.suarez.expenses.transaction.BudgetTransactionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AccountBootstrapService {

    private final AccountService accountService;
    private final BudgetTransactionRepository budgetTransactionRepository;

    public AccountBootstrapService(AccountService accountService, BudgetTransactionRepository budgetTransactionRepository) {
        this.accountService = accountService;
        this.budgetTransactionRepository = budgetTransactionRepository;
    }

    @PostConstruct
    @Transactional
    public void backfillLegacyTransactions() {
        Account defaultAccount = accountService.getOrCreateDefault();
        List<BudgetTransaction> transactionsWithoutAccount = budgetTransactionRepository.findByAccountIsNull();
        if (transactionsWithoutAccount.isEmpty()) {
            return;
        }
        for (BudgetTransaction transaction : transactionsWithoutAccount) {
            transaction.setAccount(defaultAccount);
        }
        budgetTransactionRepository.saveAll(transactionsWithoutAccount);
    }
}
