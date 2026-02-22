package com.suarez.expenses.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BudgetTransactionRepository extends JpaRepository<BudgetTransaction, Long> {
    List<BudgetTransaction> findByBudgetMonthIdAndTransactionTypeOrderByTxnDateDescIdDesc(Long budgetMonthId, TransactionType type);

    List<BudgetTransaction> findByBudgetMonthIdAndAccountIdAndTransactionTypeOrderByTxnDateDescIdDesc(Long budgetMonthId, Long accountId, TransactionType type);

    List<BudgetTransaction> findByBudgetMonthId(Long budgetMonthId);

    List<BudgetTransaction> findByBudgetMonthIdAndAccountId(Long budgetMonthId, Long accountId);

    Optional<BudgetTransaction> findByIdAndBudgetMonthIdAndTransactionType(Long id, Long budgetMonthId, TransactionType type);

    Optional<BudgetTransaction> findByIdAndBudgetMonthIdAndAccountIdAndTransactionType(Long id, Long budgetMonthId, Long accountId, TransactionType type);

    List<BudgetTransaction> findByAccountIsNull();

    List<BudgetTransaction> findByAccountIdAndSourceExternalIdIn(Long accountId, Collection<String> sourceExternalIds);

    List<BudgetTransaction> findByAccountIdAndDedupeFingerprintIn(Long accountId, Collection<String> dedupeFingerprints);

    List<BudgetTransaction> findByAccountIdAndTxnDateBetween(Long accountId, LocalDate fromDate, LocalDate toDate);

    boolean existsByCategoryId(Long categoryId);
}
