package com.suarez.expenses.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_account_name", columnNames = {"name"})
)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "institution_name", length = 120)
    private String institutionName;

    @Column(name = "last4", length = 4)
    private String last4;

    @Column(nullable = false)
    private boolean active = true;

    protected Account() {
    }

    public Account(String name, String institutionName, String last4, boolean active) {
        this.name = name;
        this.institutionName = institutionName;
        this.last4 = last4;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public String getLast4() {
        return last4;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public void setLast4(String last4) {
        this.last4 = last4;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
