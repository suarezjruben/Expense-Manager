package com.suarez.expenses.statementimport;

import com.suarez.expenses.account.Account;
import jakarta.persistence.*;

@Entity
@Table(
        name = "account_csv_header_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_account_csv_header_mappings_account",
                columnNames = {"account_id"}
        )
)
public class AccountCsvHeaderMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "date_column_index", nullable = false)
    private int dateColumnIndex;

    @Column(name = "amount_column_index", nullable = false)
    private int amountColumnIndex;

    @Column(name = "description_column_index", nullable = false)
    private int descriptionColumnIndex;

    @Column(name = "category_column_index")
    private Integer categoryColumnIndex;

    @Column(name = "external_id_column_index")
    private Integer externalIdColumnIndex;

    protected AccountCsvHeaderMapping() {
    }

    public AccountCsvHeaderMapping(
            Account account,
            int dateColumnIndex,
            int amountColumnIndex,
            int descriptionColumnIndex,
            Integer categoryColumnIndex,
            Integer externalIdColumnIndex
    ) {
        this.account = account;
        this.dateColumnIndex = dateColumnIndex;
        this.amountColumnIndex = amountColumnIndex;
        this.descriptionColumnIndex = descriptionColumnIndex;
        this.categoryColumnIndex = categoryColumnIndex;
        this.externalIdColumnIndex = externalIdColumnIndex;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public int getDateColumnIndex() {
        return dateColumnIndex;
    }

    public int getAmountColumnIndex() {
        return amountColumnIndex;
    }

    public int getDescriptionColumnIndex() {
        return descriptionColumnIndex;
    }

    public Integer getCategoryColumnIndex() {
        return categoryColumnIndex;
    }

    public Integer getExternalIdColumnIndex() {
        return externalIdColumnIndex;
    }

    public void updateFrom(CsvColumnMapping mapping) {
        this.dateColumnIndex = mapping.dateColumnIndex();
        this.amountColumnIndex = mapping.amountColumnIndex();
        this.descriptionColumnIndex = mapping.descriptionColumnIndex();
        this.categoryColumnIndex = mapping.categoryColumnIndex();
        this.externalIdColumnIndex = mapping.externalIdColumnIndex();
    }

    public CsvColumnMapping toMapping() {
        return new CsvColumnMapping(
                dateColumnIndex,
                amountColumnIndex,
                descriptionColumnIndex,
                categoryColumnIndex,
                externalIdColumnIndex
        );
    }
}
