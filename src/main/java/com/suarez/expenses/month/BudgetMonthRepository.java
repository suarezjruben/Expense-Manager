package com.suarez.expenses.month;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface BudgetMonthRepository extends JpaRepository<BudgetMonth, Long> {
    Optional<BudgetMonth> findByMonthStart(LocalDate monthStart);
}

