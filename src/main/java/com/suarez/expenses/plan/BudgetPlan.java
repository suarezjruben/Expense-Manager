package com.suarez.expenses.plan;

import com.suarez.expenses.category.Category;
import com.suarez.expenses.month.BudgetMonth;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "budget_plans",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_budget_plans_month_category",
                columnNames = {"budget_month_id", "category_id"}
        )
)
public class BudgetPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_month_id", nullable = false)
    private BudgetMonth budgetMonth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "planned_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal plannedAmount;

    protected BudgetPlan() {
    }

    public BudgetPlan(BudgetMonth budgetMonth, Category category, BigDecimal plannedAmount) {
        this.budgetMonth = budgetMonth;
        this.category = category;
        this.plannedAmount = plannedAmount;
    }

    public Long getId() {
        return id;
    }

    public BudgetMonth getBudgetMonth() {
        return budgetMonth;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }
}

