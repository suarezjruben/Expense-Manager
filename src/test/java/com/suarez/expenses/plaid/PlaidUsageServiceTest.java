package com.suarez.expenses.plaid;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "plaid.usage.free-monthly-call-limit=10",
        "plaid.usage.warning-threshold-percent=80"
})
@Transactional
class PlaidUsageServiceTest {

    @Autowired
    private PlaidUsageService plaidUsageService;

    @Test
    void shouldWarnWhenUsageReachesConfiguredThreshold() {
        PlaidUsageStatusDto status = null;
        for (int i = 0; i < 8; i++) {
            status = plaidUsageService.recordTransactionsApiCall();
        }

        assertThat(status).isNotNull();
        assertThat(status.callsUsed()).isEqualTo(8);
        assertThat(status.freeLimit()).isEqualTo(10);
        assertThat(status.warningThreshold()).isEqualTo(8);
        assertThat(status.warning()).isTrue();
        assertThat(status.exhausted()).isFalse();
        assertThat(status.message()).contains("close to the free-tier limit");
    }

    @Test
    void shouldMarkUsageAsExhaustedWhenLimitIsReached() {
        PlaidUsageStatusDto status = null;
        for (int i = 0; i < 10; i++) {
            status = plaidUsageService.recordTransactionsApiCall();
        }

        assertThat(status).isNotNull();
        assertThat(status.callsUsed()).isEqualTo(10);
        assertThat(status.remainingCalls()).isEqualTo(0);
        assertThat(status.warning()).isTrue();
        assertThat(status.exhausted()).isTrue();
        assertThat(status.message()).contains("usage limit reached");
    }
}
