package com.suarez.expenses.plaid;

import java.util.List;

public record PlaidConnectionsResponseDto(
        List<PlaidConnectionDto> connections,
        PlaidUsageStatusDto usage
) {
}
