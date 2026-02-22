package com.suarez.expenses.statementimport;

import jakarta.persistence.*;

@Entity
@Table(
        name = "import_issues",
        indexes = {
                @Index(name = "idx_import_issue_batch", columnList = "import_batch_id"),
                @Index(name = "idx_import_issue_batch_severity", columnList = "import_batch_id,severity")
        }
)
public class ImportIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_batch_id", nullable = false)
    private ImportBatch importBatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ImportIssueSeverity severity;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(nullable = false, length = 500)
    private String message;

    protected ImportIssue() {
    }

    public ImportIssue(ImportBatch importBatch, ImportIssueSeverity severity, Integer rowNumber, String message) {
        this.importBatch = importBatch;
        this.severity = severity;
        this.rowNumber = rowNumber;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public ImportBatch getImportBatch() {
        return importBatch;
    }

    public ImportIssueSeverity getSeverity() {
        return severity;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public String getMessage() {
        return message;
    }
}
