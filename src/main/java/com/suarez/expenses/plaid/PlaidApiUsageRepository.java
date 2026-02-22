package com.suarez.expenses.plaid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaidApiUsageRepository extends JpaRepository<PlaidApiUsage, Long> {
    Optional<PlaidApiUsage> findByMonthKeyAndProduct(String monthKey, String product);
}
