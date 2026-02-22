package com.suarez.expenses.plaid;

import com.suarez.expenses.statementimport.ImportSummaryDto;

public record PlaidSyncResponseDto(
        ImportSummaryDto summary,
        int fetchedAdded,
        int skippedPending,
        int skippedOtherAccounts,
        int modifiedIgnored,
        int removedIgnored,
        PlaidUsageStatusDto usage
) {
}
