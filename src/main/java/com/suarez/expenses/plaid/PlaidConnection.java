package com.suarez.expenses.plaid;

import com.suarez.expenses.account.Account;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "plaid_connections",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plaid_connection_account_plaid_account",
                columnNames = {"account_id", "plaid_account_id"}
        ),
        indexes = {
                @Index(name = "idx_plaid_connection_account", columnList = "account_id"),
                @Index(name = "idx_plaid_connection_item", columnList = "plaid_item_id")
        }
)
public class PlaidConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "plaid_item_id", nullable = false, length = 120)
    private String plaidItemId;

    @Column(name = "plaid_account_id", nullable = false, length = 120)
    private String plaidAccountId;

    @Column(name = "plaid_access_token", nullable = false, length = 512)
    private String plaidAccessToken;

    @Column(name = "plaid_account_name", nullable = false, length = 120)
    private String plaidAccountName;

    @Column(name = "institution_name", length = 120)
    private String institutionName;

    @Column(name = "mask", length = 8)
    private String mask;

    @Column(name = "transactions_cursor", length = 255)
    private String transactionsCursor;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected PlaidConnection() {
    }

    public PlaidConnection(
            Account account,
            String plaidItemId,
            String plaidAccountId,
            String plaidAccessToken,
            String plaidAccountName,
            String institutionName,
            String mask
    ) {
        this.account = account;
        this.plaidItemId = plaidItemId;
        this.plaidAccountId = plaidAccountId;
        this.plaidAccessToken = plaidAccessToken;
        this.plaidAccountName = plaidAccountName;
        this.institutionName = institutionName;
        this.mask = mask;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getPlaidItemId() {
        return plaidItemId;
    }

    public String getPlaidAccountId() {
        return plaidAccountId;
    }

    public String getPlaidAccessToken() {
        return plaidAccessToken;
    }

    public String getPlaidAccountName() {
        return plaidAccountName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public String getMask() {
        return mask;
    }

    public String getTransactionsCursor() {
        return transactionsCursor;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void updateCredentials(
            String plaidItemId,
            String plaidAccessToken,
            String plaidAccountName,
            String institutionName,
            String mask
    ) {
        this.plaidItemId = plaidItemId;
        this.plaidAccessToken = plaidAccessToken;
        this.plaidAccountName = plaidAccountName;
        this.institutionName = institutionName;
        this.mask = mask;
        this.active = true;
        this.transactionsCursor = null;
        this.lastSyncedAt = null;
    }

    public void updateCursor(String transactionsCursor, Instant lastSyncedAt) {
        this.transactionsCursor = transactionsCursor;
        this.lastSyncedAt = lastSyncedAt;
    }
}
