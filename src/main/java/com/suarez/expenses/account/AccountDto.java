package com.suarez.expenses.account;

public record AccountDto(
        Long id,
        String name,
        String institutionName,
        String last4,
        boolean active
) {
    public static AccountDto from(Account account) {
        return new AccountDto(
                account.getId(),
                account.getName(),
                account.getInstitutionName(),
                account.getLast4(),
                account.isActive()
        );
    }
}
