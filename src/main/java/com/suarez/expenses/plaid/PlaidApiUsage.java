package com.suarez.expenses.plaid;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "plaid_api_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plaid_usage_month_product",
                columnNames = {"month_key", "product"}
        ),
        indexes = {
                @Index(name = "idx_plaid_usage_month", columnList = "month_key"),
                @Index(name = "idx_plaid_usage_product", columnList = "product")
        }
)
public class PlaidApiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month_key", nullable = false, length = 7)
    private String monthKey;

    @Column(nullable = false, length = 40)
    private String product;

    @Column(name = "call_count", nullable = false)
    private int callCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaidApiUsage() {
    }

    public PlaidApiUsage(String monthKey, String product) {
        this.monthKey = monthKey;
        this.product = product;
        this.callCount = 0;
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

    public String getMonthKey() {
        return monthKey;
    }

    public String getProduct() {
        return product;
    }

    public int getCallCount() {
        return callCount;
    }

    public void incrementCallCount() {
        callCount++;
    }
}
