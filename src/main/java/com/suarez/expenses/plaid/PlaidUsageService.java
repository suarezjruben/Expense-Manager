package com.suarez.expenses.plaid;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
public class PlaidUsageService {

    public static final String TRANSACTIONS_PRODUCT = "transactions";

    private final PlaidApiUsageRepository plaidApiUsageRepository;
    private final PlaidProperties plaidProperties;

    public PlaidUsageService(PlaidApiUsageRepository plaidApiUsageRepository, PlaidProperties plaidProperties) {
        this.plaidApiUsageRepository = plaidApiUsageRepository;
        this.plaidProperties = plaidProperties;
    }

    @Transactional
    public PlaidUsageStatusDto recordTransactionsApiCall() {
        return recordApiCall(TRANSACTIONS_PRODUCT);
    }

    @Transactional(readOnly = true)
    public PlaidUsageStatusDto currentTransactionsUsage() {
        return currentUsage(TRANSACTIONS_PRODUCT);
    }

    @Transactional
    public PlaidUsageStatusDto recordApiCall(String product) {
        String monthKey = currentMonthKey();
        PlaidApiUsage usage = plaidApiUsageRepository.findByMonthKeyAndProduct(monthKey, product)
                .orElseGet(() -> plaidApiUsageRepository.save(new PlaidApiUsage(monthKey, product)));
        usage.incrementCallCount();
        PlaidApiUsage saved = plaidApiUsageRepository.save(usage);
        return toStatus(saved.getMonthKey(), saved.getProduct(), saved.getCallCount());
    }

    @Transactional(readOnly = true)
    public PlaidUsageStatusDto currentUsage(String product) {
        String monthKey = currentMonthKey();
        int callsUsed = plaidApiUsageRepository.findByMonthKeyAndProduct(monthKey, product)
                .map(PlaidApiUsage::getCallCount)
                .orElse(0);
        return toStatus(monthKey, product, callsUsed);
    }

    private PlaidUsageStatusDto toStatus(String month, String product, int callsUsed) {
        int freeLimit = Math.max(plaidProperties.getUsage().getFreeMonthlyCallLimit(), 1);
        int warningThreshold = resolveWarningThreshold(freeLimit, plaidProperties.getUsage().getWarningThresholdPercent());
        int remainingCalls = Math.max(freeLimit - callsUsed, 0);
        boolean exhausted = callsUsed >= freeLimit;
        boolean warning = exhausted || callsUsed >= warningThreshold;

        String message = null;
        if (exhausted) {
            message = "Plaid " + product + " free-tier usage limit reached for " + month
                    + " (" + callsUsed + "/" + freeLimit + " tracked calls).";
        } else if (warning) {
            message = "Plaid " + product + " usage is close to the free-tier limit for " + month
                    + " (" + callsUsed + "/" + freeLimit + " tracked calls).";
        }

        return new PlaidUsageStatusDto(
                month,
                product,
                callsUsed,
                freeLimit,
                warningThreshold,
                remainingCalls,
                warning,
                exhausted,
                message
        );
    }

    private int resolveWarningThreshold(int freeLimit, int warningThresholdPercent) {
        int clamped = Math.max(0, Math.min(100, warningThresholdPercent));
        if (clamped == 0) {
            return freeLimit + 1;
        }
        if (clamped == 100) {
            return freeLimit;
        }
        return (int) Math.ceil((freeLimit * clamped) / 100.0d);
    }

    private String currentMonthKey() {
        return YearMonth.now().toString();
    }
}
