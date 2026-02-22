package com.suarez.expenses.plaid;

import com.suarez.expenses.account.Account;
import com.suarez.expenses.account.AccountService;
import com.suarez.expenses.common.BadRequestException;
import com.suarez.expenses.common.NotFoundException;
import com.suarez.expenses.statementimport.ImportSummaryDto;
import com.suarez.expenses.statementimport.NormalizedStatementRow;
import com.suarez.expenses.statementimport.StatementFileType;
import com.suarez.expenses.statementimport.StatementImportService;
import com.suarez.expenses.statementimport.StatementIssue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class PlaidService {

    private static final int MAX_SYNC_PAGES = 1_000;

    private final AccountService accountService;
    private final PlaidConnectionRepository plaidConnectionRepository;
    private final PlaidHttpClient plaidHttpClient;
    private final PlaidUsageService plaidUsageService;
    private final PlaidProperties plaidProperties;
    private final StatementImportService statementImportService;

    public PlaidService(
            AccountService accountService,
            PlaidConnectionRepository plaidConnectionRepository,
            PlaidHttpClient plaidHttpClient,
            PlaidUsageService plaidUsageService,
            PlaidProperties plaidProperties,
            StatementImportService statementImportService
    ) {
        this.accountService = accountService;
        this.plaidConnectionRepository = plaidConnectionRepository;
        this.plaidHttpClient = plaidHttpClient;
        this.plaidUsageService = plaidUsageService;
        this.plaidProperties = plaidProperties;
        this.statementImportService = statementImportService;
    }

    @Transactional(readOnly = true)
    public PlaidCreateLinkTokenResponseDto createLinkToken(Long accountId) {
        accountService.resolveAccount(accountId);
        String linkToken = plaidHttpClient.createLinkToken("account-" + accountId);
        return new PlaidCreateLinkTokenResponseDto(linkToken, plaidUsageService.currentTransactionsUsage());
    }

    @Transactional
    public PlaidExchangeResponseDto exchangePublicToken(Long accountId, PlaidExchangePublicTokenRequest request) {
        Account account = accountService.resolveAccount(accountId);
        String publicToken = normalizeOptional(request.publicToken());
        if (publicToken == null) {
            throw new BadRequestException("publicToken is required");
        }

        PlaidTokenExchangeResult exchange = plaidHttpClient.exchangePublicToken(publicToken);
        List<PlaidAccountInfo> plaidAccounts = plaidHttpClient.getAccounts(exchange.accessToken());
        PlaidAccountInfo selectedAccount = resolvePlaidAccount(plaidAccounts, request.plaidAccountId());

        String displayName = firstNonBlank(
                normalizeOptional(request.plaidAccountName()),
                normalizeOptional(selectedAccount.name()),
                "Plaid Account"
        );
        String institutionName = firstNonBlank(
                normalizeOptional(request.institutionName()),
                normalizeOptional(account.getInstitutionName())
        );
        String mask = firstNonBlank(
                normalizeOptional(request.mask()),
                normalizeOptional(selectedAccount.mask()),
                normalizeOptional(account.getLast4())
        );

        PlaidConnection connection = plaidConnectionRepository.findByAccountIdAndPlaidAccountId(accountId, selectedAccount.accountId())
                .map(existing -> {
                    existing.updateCredentials(
                            exchange.itemId(),
                            exchange.accessToken(),
                            displayName,
                            institutionName,
                            mask
                    );
                    return existing;
                })
                .orElseGet(() -> new PlaidConnection(
                        account,
                        exchange.itemId(),
                        selectedAccount.accountId(),
                        exchange.accessToken(),
                        displayName,
                        institutionName,
                        mask
                ));

        PlaidConnection saved = plaidConnectionRepository.save(connection);
        return new PlaidExchangeResponseDto(
                PlaidConnectionDto.from(saved),
                plaidUsageService.currentTransactionsUsage()
        );
    }

    @Transactional(readOnly = true)
    public PlaidConnectionsResponseDto listConnections(Long accountId) {
        accountService.resolveAccount(accountId);
        List<PlaidConnectionDto> connections = plaidConnectionRepository.findByAccountIdAndActiveTrueOrderByIdAsc(accountId).stream()
                .map(PlaidConnectionDto::from)
                .toList();
        return new PlaidConnectionsResponseDto(connections, plaidUsageService.currentTransactionsUsage());
    }

    @Transactional
    public PlaidSyncResponseDto syncConnection(Long accountId, Long connectionId) {
        accountService.resolveAccount(accountId);
        PlaidConnection connection = plaidConnectionRepository.findByIdAndAccountIdAndActiveTrue(connectionId, accountId)
                .orElseThrow(() -> new NotFoundException("Plaid connection not found: " + connectionId));

        String accessToken = normalizeOptional(connection.getPlaidAccessToken());
        if (accessToken == null) {
            throw new BadRequestException("Plaid connection is missing an access token");
        }

        String cursor = normalizeOptional(connection.getTransactionsCursor());
        List<PlaidTransaction> added = new ArrayList<>();
        int modifiedCount = 0;
        int removedCount = 0;
        int pageCount = 0;
        boolean hasMore;
        PlaidUsageStatusDto usageStatus = plaidUsageService.currentTransactionsUsage();

        do {
            pageCount++;
            if (pageCount > MAX_SYNC_PAGES) {
                throw new IllegalStateException("Plaid sync exceeded maximum page count");
            }

            PlaidTransactionsSyncPage page = plaidHttpClient.syncTransactions(accessToken, cursor, resolveSyncPageSize());
            usageStatus = plaidUsageService.recordTransactionsApiCall();

            added.addAll(page.added());
            modifiedCount += page.modifiedCount();
            removedCount += page.removedCount();
            cursor = normalizeOptional(page.nextCursor());
            hasMore = page.hasMore();
        } while (hasMore);

        List<NormalizedStatementRow> rows = new ArrayList<>();
        List<StatementIssue> issues = new ArrayList<>();

        int fetchedAdded = 0;
        int skippedPending = 0;
        int skippedOtherAccounts = 0;
        int rowNumber = 1;
        String connectionPlaidAccountId = normalizeOptional(connection.getPlaidAccountId());

        for (PlaidTransaction transaction : added) {
            if (connectionPlaidAccountId != null && !Objects.equals(connectionPlaidAccountId, normalizeOptional(transaction.accountId()))) {
                skippedOtherAccounts++;
                continue;
            }
            fetchedAdded++;

            if (transaction.pending()) {
                skippedPending++;
                continue;
            }
            if (transaction.date() == null) {
                issues.add(StatementIssue.error(rowNumber, "Plaid transaction is missing a date"));
                rowNumber++;
                continue;
            }
            if (transaction.amount() == null) {
                issues.add(StatementIssue.error(rowNumber, "Plaid transaction is missing an amount"));
                rowNumber++;
                continue;
            }

            // Plaid amounts are positive for money leaving the account; importer expects negatives for expenses.
            BigDecimal signedAmount = transaction.amount().negate();
            String description = firstNonBlank(
                    normalizeOptional(transaction.merchantName()),
                    normalizeOptional(transaction.name()),
                    "Plaid transaction"
            );
            String sourceCategory = normalizePlaidCategory(transaction.personalFinanceCategoryPrimary());

            rows.add(new NormalizedStatementRow(
                    rowNumber,
                    transaction.date(),
                    signedAmount,
                    description,
                    normalizeOptional(transaction.transactionId()),
                    sourceCategory
            ));
            rowNumber++;
        }

        if (skippedPending > 0) {
            issues.add(StatementIssue.warning(null, "Skipped " + skippedPending + " pending Plaid transaction(s)"));
        }
        if (modifiedCount > 0) {
            issues.add(StatementIssue.warning(null, "Plaid returned " + modifiedCount + " modified transaction(s), which are ignored in this sync"));
        }
        if (removedCount > 0) {
            issues.add(StatementIssue.warning(null, "Plaid returned " + removedCount + " removed transaction(s), which are ignored in this sync"));
        }
        if (skippedOtherAccounts > 0) {
            issues.add(StatementIssue.warning(null, "Skipped " + skippedOtherAccounts + " transaction(s) from other Plaid accounts in the same Item"));
        }

        ImportSummaryDto summary = statementImportService.importNormalizedRows(
                accountId,
                buildSourceName(connection),
                StatementFileType.PLAID,
                rows,
                issues
        );

        connection.updateCursor(cursor, Instant.now());
        plaidConnectionRepository.save(connection);

        return new PlaidSyncResponseDto(
                summary,
                fetchedAdded,
                skippedPending,
                skippedOtherAccounts,
                modifiedCount,
                removedCount,
                usageStatus
        );
    }

    private PlaidAccountInfo resolvePlaidAccount(List<PlaidAccountInfo> plaidAccounts, String requestedPlaidAccountId) {
        if (plaidAccounts == null || plaidAccounts.isEmpty()) {
            throw new BadRequestException("Plaid Item does not include any accounts");
        }

        String requested = normalizeOptional(requestedPlaidAccountId);
        if (requested != null) {
            return plaidAccounts.stream()
                    .filter(account -> requested.equals(account.accountId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Selected Plaid account was not found in Item"));
        }

        if (plaidAccounts.size() > 1) {
            throw new BadRequestException("Multiple Plaid accounts were returned. Provide plaidAccountId from Link metadata.");
        }
        return plaidAccounts.getFirst();
    }

    private int resolveSyncPageSize() {
        int configured = plaidProperties.getSyncPageSize();
        if (configured <= 0) {
            return 100;
        }
        return Math.min(configured, 500);
    }

    private String buildSourceName(PlaidConnection connection) {
        String accountName = normalizeOptional(connection.getPlaidAccountName());
        return accountName == null ? "Plaid Sync" : "Plaid Sync - " + accountName;
    }

    private String normalizePlaidCategory(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }

        String words = normalized.toLowerCase(Locale.ROOT).replace("_", " ");
        String[] parts = words.split(" ");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (part.length() == 1) {
                result.add(part.toUpperCase(Locale.ROOT));
            } else {
                result.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
            }
        }
        return result.isEmpty() ? null : String.join(" ", result);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeOptional(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }
}
