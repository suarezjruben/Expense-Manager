package com.suarez.expenses.statementimport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountCsvHeaderMappingRepository extends JpaRepository<AccountCsvHeaderMapping, Long> {
    Optional<AccountCsvHeaderMapping> findByAccountId(Long accountId);
}
