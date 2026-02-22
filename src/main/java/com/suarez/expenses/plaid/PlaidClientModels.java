package com.suarez.expenses.plaid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record PlaidTokenExchangeResult(
        String accessToken,
        String itemId
) {
}

record PlaidAccountInfo(
        String accountId,
        String name,
        String mask,
        String type,
        String subtype
) {
}

record PlaidTransaction(
        String transactionId,
        String accountId,
        LocalDate date,
        BigDecimal amount,
        String name,
        String merchantName,
        String personalFinanceCategoryPrimary,
        boolean pending
) {
}

record PlaidTransactionsSyncPage(
        List<PlaidTransaction> added,
        int modifiedCount,
        int removedCount,
        String nextCursor,
        boolean hasMore
) {
}
