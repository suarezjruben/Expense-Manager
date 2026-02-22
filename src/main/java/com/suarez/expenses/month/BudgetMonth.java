package com.suarez.expenses.month;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "budget_months",
        uniqueConstraints = @UniqueConstraint(name = "uk_budget_month_start", columnNames = {"month_start"})
)
public class BudgetMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "starting_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal startingBalance;

    protected BudgetMonth() {
    }

    public BudgetMonth(LocalDate monthStart, BigDecimal startingBalance) {
        this.monthStart = monthStart;
        this.startingBalance = startingBalance;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getMonthStart() {
        return monthStart;
    }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }
}

