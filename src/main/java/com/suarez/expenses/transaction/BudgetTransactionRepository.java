package com.suarez.expenses.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetTransactionRepository extends JpaRepository<BudgetTransaction, Long> {
    List<BudgetTransaction> findByBudgetMonthIdAndTransactionTypeOrderByTxnDateDescIdDesc(Long budgetMonthId, TransactionType type);

    List<BudgetTransaction> findByBudgetMonthId(Long budgetMonthId);

    Optional<BudgetTransaction> findByIdAndBudgetMonthIdAndTransactionType(Long id, Long budgetMonthId, TransactionType type);

    boolean existsByCategoryId(Long categoryId);
}

