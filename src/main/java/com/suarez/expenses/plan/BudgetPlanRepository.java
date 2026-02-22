package com.suarez.expenses.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {
    List<BudgetPlan> findByBudgetMonthId(Long budgetMonthId);

    Optional<BudgetPlan> findByBudgetMonthIdAndCategoryId(Long budgetMonthId, Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}

