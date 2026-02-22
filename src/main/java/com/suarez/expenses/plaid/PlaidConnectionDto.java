package com.suarez.expenses.plaid;

import java.time.Instant;

public record PlaidConnectionDto(
        Long id,
        Long accountId,
        String plaidItemId,
        String plaidAccountId,
        String plaidAccountName,
        String institutionName,
        String mask,
        String transactionsCursor,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant lastSyncedAt
) {
    public static PlaidConnectionDto from(PlaidConnection connection) {
        return new PlaidConnectionDto(
                connection.getId(),
                connection.getAccount().getId(),
                connection.getPlaidItemId(),
                connection.getPlaidAccountId(),
                connection.getPlaidAccountName(),
                connection.getInstitutionName(),
                connection.getMask(),
                connection.getTransactionsCursor(),
                connection.isActive(),
                connection.getCreatedAt(),
                connection.getUpdatedAt(),
                connection.getLastSyncedAt()
        );
    }
}
