package com.suarez.expenses.transaction;

import com.suarez.expenses.category.Category;
import com.suarez.expenses.month.BudgetMonth;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budget_transactions", indexes = {
        @Index(name = "idx_transactions_month_type", columnList = "budget_month_id,transaction_type"),
        @Index(name = "idx_transactions_month_date", columnList = "budget_month_id,txn_date")
})
public class BudgetTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_month_id", nullable = false)
    private BudgetMonth budgetMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 300)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected BudgetTransaction() {
    }

    public BudgetTransaction(
            BudgetMonth budgetMonth,
            TransactionType transactionType,
            LocalDate txnDate,
            BigDecimal amount,
            String description,
            Category category
    ) {
        this.budgetMonth = budgetMonth;
        this.transactionType = transactionType;
        this.txnDate = txnDate;
        this.amount = amount;
        this.description = description;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public BudgetMonth getBudgetMonth() {
        return budgetMonth;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public void setTxnDate(LocalDate txnDate) {
        this.txnDate = txnDate;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}

