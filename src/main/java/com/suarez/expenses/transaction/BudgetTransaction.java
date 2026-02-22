package com.suarez.expenses.transaction;

import com.suarez.expenses.account.Account;
import com.suarez.expenses.category.Category;
import com.suarez.expenses.month.BudgetMonth;
import com.suarez.expenses.statementimport.ImportBatch;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "budget_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transactions_account_external_id",
                        columnNames = {"account_id", "source_external_id"}
                ),
                @UniqueConstraint(
                        name = "uk_transactions_account_fingerprint",
                        columnNames = {"account_id", "dedupe_fingerprint"}
                )
        },
        indexes = {
                @Index(name = "idx_transactions_month_type", columnList = "budget_month_id,transaction_type"),
                @Index(name = "idx_transactions_month_date", columnList = "budget_month_id,txn_date"),
                @Index(name = "idx_transactions_account_month_type", columnList = "account_id,budget_month_id,transaction_type"),
                @Index(name = "idx_transactions_account_date", columnList = "account_id,txn_date"),
                @Index(name = "idx_transactions_import_batch", columnList = "import_batch_id")
        }
)
public class BudgetTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_month_id", nullable = false)
    private BudgetMonth budgetMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

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

    @Column(name = "source_external_id", length = 200)
    private String sourceExternalId;

    @Column(name = "dedupe_fingerprint", length = 128)
    private String dedupeFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    private ImportBatch importBatch;

    protected BudgetTransaction() {
    }

    public BudgetTransaction(
            BudgetMonth budgetMonth,
            Account account,
            TransactionType transactionType,
            LocalDate txnDate,
            BigDecimal amount,
            String description,
            Category category
    ) {
        this.budgetMonth = budgetMonth;
        this.account = account;
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

    public Account getAccount() {
        return account;
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

    public String getSourceExternalId() {
        return sourceExternalId;
    }

    public String getDedupeFingerprint() {
        return dedupeFingerprint;
    }

    public ImportBatch getImportBatch() {
        return importBatch;
    }

    public void setAccount(Account account) {
        this.account = account;
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

    public void setSourceExternalId(String sourceExternalId) {
        this.sourceExternalId = sourceExternalId;
    }

    public void setDedupeFingerprint(String dedupeFingerprint) {
        this.dedupeFingerprint = dedupeFingerprint;
    }

    public void setImportBatch(ImportBatch importBatch) {
        this.importBatch = importBatch;
    }
}
