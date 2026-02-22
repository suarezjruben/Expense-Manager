package com.suarez.expenses.plaid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaidConnectionRepository extends JpaRepository<PlaidConnection, Long> {
    List<PlaidConnection> findByAccountIdAndActiveTrueOrderByIdAsc(Long accountId);

    Optional<PlaidConnection> findByIdAndAccountIdAndActiveTrue(Long id, Long accountId);

    Optional<PlaidConnection> findByAccountIdAndPlaidAccountId(Long accountId, String plaidAccountId);
}
