package com.suarez.expenses.statementimport;

import com.suarez.expenses.account.Account;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "import_batches",
        indexes = {
                @Index(name = "idx_import_batch_account_created", columnList = "account_id,created_at"),
                @Index(name = "idx_import_batch_status", columnList = "status")
        }
)
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private StatementFileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ImportBatchStatus status;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "skipped_duplicates_count", nullable = false)
    private int skippedDuplicatesCount;

    @Column(name = "parse_error_count", nullable = false)
    private int parseErrorCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ImportBatch() {
    }

    public ImportBatch(Account account, String fileName, StatementFileType fileType, ImportBatchStatus status, Instant createdAt) {
        this.account = account;
        this.fileName = fileName;
        this.fileType = fileType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getFileName() {
        return fileName;
    }

    public StatementFileType getFileType() {
        return fileType;
    }

    public ImportBatchStatus getStatus() {
        return status;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public int getSkippedDuplicatesCount() {
        return skippedDuplicatesCount;
    }

    public int getParseErrorCount() {
        return parseErrorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void complete(
            ImportBatchStatus status,
            int insertedCount,
            int skippedDuplicatesCount,
            int parseErrorCount,
            int warningCount,
            Instant completedAt
    ) {
        this.status = status;
        this.insertedCount = insertedCount;
        this.skippedDuplicatesCount = skippedDuplicatesCount;
        this.parseErrorCount = parseErrorCount;
        this.warningCount = warningCount;
        this.completedAt = completedAt;
    }
}
