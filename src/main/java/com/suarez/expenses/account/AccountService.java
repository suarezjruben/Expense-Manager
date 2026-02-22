package com.suarez.expenses.account;

import com.suarez.expenses.common.BadRequestException;
import com.suarez.expenses.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    public static final String DEFAULT_ACCOUNT_NAME = "Primary";

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountDto> list(boolean includeInactive) {
        List<Account> accounts = includeInactive
                ? accountRepository.findAllByOrderByNameAsc()
                : accountRepository.findByActiveTrueOrderByNameAsc();
        return accounts.stream().map(AccountDto::from).toList();
    }

    @Transactional
    public AccountDto create(CreateAccountRequest request) {
        String name = request.name().trim();
        accountRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new BadRequestException("Account already exists: " + name);
        });
        Account account = new Account(
                name,
                normalizeOptional(request.institutionName()),
                normalizeOptional(request.last4()),
                true
        );
        return AccountDto.from(accountRepository.save(account));
    }

    @Transactional
    public Account resolveAccount(Long accountId) {
        if (accountId == null) {
            return getOrCreateDefault();
        }
        return accountRepository.findByIdAndActiveTrue(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
    }

    @Transactional
    public Account getOrCreateDefault() {
        return accountRepository.findByNameIgnoreCase(DEFAULT_ACCOUNT_NAME)
                .orElseGet(() -> accountRepository.save(new Account(DEFAULT_ACCOUNT_NAME, null, null, true)));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
