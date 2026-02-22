package com.suarez.expenses.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findAllByOrderByNameAsc();

    List<Account> findByActiveTrueOrderByNameAsc();

    Optional<Account> findByNameIgnoreCase(String name);

    Optional<Account> findByIdAndActiveTrue(Long id);
}
