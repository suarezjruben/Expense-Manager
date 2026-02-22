package com.suarez.expenses.statementimport;

import com.suarez.expenses.account.Account;
import com.suarez.expenses.account.AccountService;
import com.suarez.expenses.category.Category;
import com.suarez.expenses.category.CategoryRepository;
import com.suarez.expenses.category.CategoryType;
import com.suarez.expenses.common.BadRequestException;
import com.suarez.expenses.month.BudgetMonth;
import com.suarez.expenses.month.BudgetMonthService;
import com.suarez.expenses.transaction.BudgetTransaction;
import com.suarez.expenses.transaction.BudgetTransactionRepository;
import com.suarez.expenses.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class StatementImportService {

    private static final String IMPORTED_EXPENSE_CATEGORY = "Imported Expense";
    private static final String IMPORTED_INCOME_CATEGORY = "Imported Income";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AccountService accountService;
    private final CategoryRepository categoryRepository;
    private final BudgetMonthService budgetMonthService;
    private final BudgetTransactionRepository budgetTransactionRepository;
    private final ImportBatchRepository importBatchRepository;
    private final ImportIssueRepository importIssueRepository;
    private final StatementParserRegistry statementParserRegistry;
    private final StatementFileTypeDetector fileTypeDetector;
    private final CsvStatementParser csvStatementParser;
    private final AccountCsvHeaderMappingRepository accountCsvHeaderMappingRepository;

    public StatementImportService(
            AccountService accountService,
            CategoryRepository categoryRepository,
            BudgetMonthService budgetMonthService,
            BudgetTransactionRepository budgetTransactionRepository,
            ImportBatchRepository importBatchRepository,
            ImportIssueRepository importIssueRepository,
            StatementParserRegistry statementParserRegistry,
            StatementFileTypeDetector fileTypeDetector,
            CsvStatementParser csvStatementParser,
            AccountCsvHeaderMappingRepository accountCsvHeaderMappingRepository
    ) {
        this.accountService = accountService;
        this.categoryRepository = categoryRepository;
        this.budgetMonthService = budgetMonthService;
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.importBatchRepository = importBatchRepository;
        this.importIssueRepository = importIssueRepository;
        this.statementParserRegistry = statementParserRegistry;
        this.fileTypeDetector = fileTypeDetector;
        this.csvStatementParser = csvStatementParser;
        this.accountCsvHeaderMappingRepository = accountCsvHeaderMappingRepository;
    }

    @Transactional
    public StatementImportResponseDto importStatement(
            Long accountId,
            MultipartFile file,
            Integer dateColumnIndex,
            Integer amountColumnIndex,
            Integer descriptionColumnIndex,
            Integer categoryColumnIndex,
            Integer externalIdColumnIndex,
            boolean saveHeaderMapping
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        Account account = accountService.resolveAccount(accountId);
        String fileName = normalizeFileName(file.getOriginalFilename());
        StatementFileType fileType = fileTypeDetector.detect(fileName);
        CsvColumnMapping requestedCsvMapping = parseCsvColumnMappingRequest(
                dateColumnIndex,
                amountColumnIndex,
                descriptionColumnIndex,
                categoryColumnIndex,
                externalIdColumnIndex
        );

        StatementParseResult parseResult;
        try (InputStream inputStream = file.getInputStream()) {
            if (fileType == StatementFileType.CSV) {
                CsvColumnMapping effectiveMapping = requestedCsvMapping != null
                        ? requestedCsvMapping
                        : loadSavedCsvMapping(account.getId());
                parseResult = csvStatementParser.parse(inputStream, effectiveMapping);
            } else {
                parseResult = statementParserRegistry.parse(fileType, inputStream);
            }
        } catch (CsvHeaderMappingRequiredException ex) {
            return StatementImportResponseDto.headerMappingRequired(ex.getPrompt());
        } catch (IOException ex) {
            ImportSummaryDto summary = completeImport(
                    account,
                    fileName,
                    fileType,
                    List.of(),
                    List.of(StatementIssue.error(null, "Unable to read uploaded file"))
            );
            return StatementImportResponseDto.completed(summary);
        } catch (RuntimeException ex) {
            ImportSummaryDto summary = completeImport(
                    account,
                    fileName,
                    fileType,
                    List.of(),
                    List.of(StatementIssue.error(null, "Unable to parse statement: " + ex.getMessage()))
            );
            return StatementImportResponseDto.completed(summary);
        }

        if (fileType == StatementFileType.CSV && requestedCsvMapping != null && saveHeaderMapping) {
            upsertSavedCsvMapping(account, requestedCsvMapping);
        }

        List<StatementIssue> allIssues = new ArrayList<>(parseResult.issues());
        ImportSummaryDto summary = completeImport(account, fileName, fileType, parseResult.rows(), allIssues);
        return StatementImportResponseDto.completed(summary);
    }

    @Transactional
    public ImportSummaryDto importNormalizedRows(
            Long accountId,
            String sourceName,
            StatementFileType fileType,
            List<NormalizedStatementRow> rows,
            List<StatementIssue> issues
    ) {
        Account account = accountService.resolveAccount(accountId);
        String normalizedSourceName = normalizeSourceName(sourceName);
        StatementFileType resolvedFileType = fileType == null ? StatementFileType.CSV : fileType;
        List<NormalizedStatementRow> safeRows = rows == null ? List.of() : rows;
        List<StatementIssue> safeIssues = issues == null ? new ArrayList<>() : new ArrayList<>(issues);
        return completeImport(account, normalizedSourceName, resolvedFileType, safeRows, safeIssues);
    }

    private ImportSummaryDto completeImport(
            Account account,
            String fileName,
            StatementFileType fileType,
            List<NormalizedStatementRow> parsedRows,
            List<StatementIssue> issues
    ) {
        ImportBatch batch = importBatchRepository.save(new ImportBatch(
                account,
                fileName,
                fileType,
                ImportBatchStatus.PROCESSING,
                Instant.now()
        ));

        Category fallbackExpenseCategory = getOrCreateImportCategory(CategoryType.EXPENSE, IMPORTED_EXPENSE_CATEGORY);
        Category fallbackIncomeCategory = getOrCreateImportCategory(CategoryType.INCOME, IMPORTED_INCOME_CATEGORY);
        Map<String, Category> categoryByTypeAndName = new HashMap<>();

        List<CandidateTransaction> candidates = normalizeRows(parsedRows, issues);

        Set<String> existingExternalIds = loadExistingExternalIds(account.getId(), candidates);
        Set<String> existingFingerprints = loadExistingFingerprints(account.getId(), candidates);
        Set<String> seenExternalIds = new HashSet<>(existingExternalIds);
        Set<String> seenFingerprints = new HashSet<>(existingFingerprints);

        Map<LocalDate, BudgetMonth> monthCache = new HashMap<>();
        List<BudgetTransaction> toInsert = new ArrayList<>();
        int skippedDuplicates = 0;

        for (CandidateTransaction candidate : candidates) {
            boolean duplicateByExternalId = candidate.externalId() != null && seenExternalIds.contains(candidate.externalId());
            boolean duplicateByFingerprint = seenFingerprints.contains(candidate.fingerprint());
            if (duplicateByExternalId || duplicateByFingerprint) {
                skippedDuplicates++;
                continue;
            }

            if (candidate.externalId() != null) {
                seenExternalIds.add(candidate.externalId());
            }
            seenFingerprints.add(candidate.fingerprint());

            BudgetMonth budgetMonth = monthCache.computeIfAbsent(
                    candidate.date().withDayOfMonth(1),
                    budgetMonthService::getOrCreate
            );

            Category fallbackCategory = candidate.type() == TransactionType.EXPENSE ? fallbackExpenseCategory : fallbackIncomeCategory;
            Category category = resolveCategoryForCandidate(
                    candidate.type(),
                    candidate.sourceCategory(),
                    fallbackCategory,
                    categoryByTypeAndName
            );

            BudgetTransaction transaction = new BudgetTransaction(
                    budgetMonth,
                    account,
                    candidate.type(),
                    candidate.date(),
                    candidate.amount(),
                    candidate.description(),
                    category
            );
            transaction.setSourceExternalId(candidate.externalId());
            transaction.setDedupeFingerprint(candidate.fingerprint());
            transaction.setImportBatch(batch);
            toInsert.add(transaction);
        }

        if (!toInsert.isEmpty()) {
            budgetTransactionRepository.saveAll(toInsert);
        }

        List<ImportIssue> persistedIssues = issues.stream()
                .map(issue -> new ImportIssue(batch, issue.severity(), issue.rowNumber(), truncate(issue.message(), 500)))
                .toList();
        if (!persistedIssues.isEmpty()) {
            importIssueRepository.saveAll(persistedIssues);
        }

        List<StatementIssue> parseErrors = issues.stream()
                .filter(issue -> issue.severity() == ImportIssueSeverity.ERROR)
                .toList();
        List<StatementIssue> warnings = issues.stream()
                .filter(issue -> issue.severity() == ImportIssueSeverity.WARNING)
                .toList();

        ImportBatchStatus status = resolveFinalStatus(parseErrors.size(), warnings.size());
        batch.complete(
                status,
                toInsert.size(),
                skippedDuplicates,
                parseErrors.size(),
                warnings.size(),
                Instant.now()
        );
        importBatchRepository.save(batch);

        return new ImportSummaryDto(
                batch.getId(),
                toInsert.size(),
                skippedDuplicates,
                parseErrors.stream().map(ImportIssueDto::from).toList(),
                warnings.stream().map(ImportIssueDto::from).toList()
        );
    }

    private List<CandidateTransaction> normalizeRows(List<NormalizedStatementRow> rows, List<StatementIssue> issues) {
        List<CandidateTransaction> candidates = new ArrayList<>();
        for (NormalizedStatementRow row : rows) {
            if (row.date() == null) {
                issues.add(StatementIssue.error(row.rowNumber(), "Row is missing a transaction date"));
                continue;
            }
            if (row.signedAmount() == null) {
                issues.add(StatementIssue.error(row.rowNumber(), "Row is missing an amount"));
                continue;
            }

            BigDecimal signedAmount = row.signedAmount().setScale(2, RoundingMode.HALF_UP);
            if (signedAmount.compareTo(ZERO) == 0) {
                issues.add(StatementIssue.warning(row.rowNumber(), "Skipped zero-amount transaction"));
                continue;
            }

            TransactionType type = signedAmount.signum() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
            BigDecimal amount = signedAmount.abs();

            String description = normalizeDescription(row.description());
            String externalId = truncate(normalizeOptional(row.externalId()), 200);
            String sourceCategory = truncate(normalizeOptional(row.sourceCategory()), 120);
            String fingerprint = buildFingerprint(row.date(), type, amount, description);

            candidates.add(new CandidateTransaction(
                    row.rowNumber(),
                    row.date(),
                    type,
                    amount,
                    description,
                    externalId,
                    sourceCategory,
                    fingerprint
            ));
        }
        return candidates;
    }

    private Set<String> loadExistingExternalIds(Long accountId, List<CandidateTransaction> candidates) {
        Set<String> candidateExternalIds = candidates.stream()
                .map(CandidateTransaction::externalId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        if (candidateExternalIds.isEmpty()) {
            return Set.of();
        }
        return budgetTransactionRepository.findByAccountIdAndSourceExternalIdIn(accountId, candidateExternalIds).stream()
                .map(BudgetTransaction::getSourceExternalId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    private Set<String> loadExistingFingerprints(Long accountId, List<CandidateTransaction> candidates) {
        Set<String> candidateFingerprints = candidates.stream()
                .map(CandidateTransaction::fingerprint)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        if (candidateFingerprints.isEmpty()) {
            return Set.of();
        }
        return budgetTransactionRepository.findByAccountIdAndDedupeFingerprintIn(accountId, candidateFingerprints).stream()
                .map(BudgetTransaction::getDedupeFingerprint)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    private Category resolveCategoryForCandidate(
            TransactionType transactionType,
            String sourceCategory,
            Category fallbackCategory,
            Map<String, Category> categoryByTypeAndName
    ) {
        String normalized = normalizeOptional(sourceCategory);
        if (normalized == null) {
            return fallbackCategory;
        }

        CategoryType categoryType = transactionType == TransactionType.EXPENSE ? CategoryType.EXPENSE : CategoryType.INCOME;
        String cacheKey = categoryType + "|" + normalized.toLowerCase(Locale.ROOT);
        return categoryByTypeAndName.computeIfAbsent(cacheKey, key ->
                categoryRepository.findByTypeAndNameIgnoreCase(categoryType, normalized)
                        .orElseGet(() -> {
                            Integer maxSort = categoryRepository.findMaxSortOrderByType(categoryType);
                            int nextSort = (maxSort == null ? 0 : maxSort) + 1;
                            return categoryRepository.save(new Category(normalized, categoryType, nextSort, true));
                        })
        );
    }

    private CsvColumnMapping loadSavedCsvMapping(Long accountId) {
        return accountCsvHeaderMappingRepository.findByAccountId(accountId)
                .map(AccountCsvHeaderMapping::toMapping)
                .orElse(null);
    }

    private void upsertSavedCsvMapping(Account account, CsvColumnMapping mapping) {
        accountCsvHeaderMappingRepository.findByAccountId(account.getId())
                .ifPresentOrElse(existing -> {
                    existing.updateFrom(mapping);
                    accountCsvHeaderMappingRepository.save(existing);
                }, () -> accountCsvHeaderMappingRepository.save(new AccountCsvHeaderMapping(
                        account,
                        mapping.dateColumnIndex(),
                        mapping.amountColumnIndex(),
                        mapping.descriptionColumnIndex(),
                        mapping.categoryColumnIndex(),
                        mapping.externalIdColumnIndex()
                )));
    }

    private CsvColumnMapping parseCsvColumnMappingRequest(
            Integer dateColumnIndex,
            Integer amountColumnIndex,
            Integer descriptionColumnIndex,
            Integer categoryColumnIndex,
            Integer externalIdColumnIndex
    ) {
        boolean anyProvided = dateColumnIndex != null
                || amountColumnIndex != null
                || descriptionColumnIndex != null
                || categoryColumnIndex != null
                || externalIdColumnIndex != null;
        if (!anyProvided) {
            return null;
        }

        if (dateColumnIndex == null || amountColumnIndex == null || descriptionColumnIndex == null) {
            throw new BadRequestException(
                    "dateColumnIndex, amountColumnIndex, and descriptionColumnIndex are required when providing CSV mapping"
            );
        }

        validateNonNegative("dateColumnIndex", dateColumnIndex);
        validateNonNegative("amountColumnIndex", amountColumnIndex);
        validateNonNegative("descriptionColumnIndex", descriptionColumnIndex);
        validateNonNegativeIfPresent("categoryColumnIndex", categoryColumnIndex);
        validateNonNegativeIfPresent("externalIdColumnIndex", externalIdColumnIndex);

        return new CsvColumnMapping(
                dateColumnIndex,
                amountColumnIndex,
                descriptionColumnIndex,
                categoryColumnIndex,
                externalIdColumnIndex
        );
    }

    private void validateNonNegativeIfPresent(String field, Integer value) {
        if (value != null) {
            validateNonNegative(field, value);
        }
    }

    private void validateNonNegative(String field, int value) {
        if (value < 0) {
            throw new BadRequestException(field + " must be >= 0");
        }
    }

    private ImportBatchStatus resolveFinalStatus(int parseErrorCount, int warningCount) {
        if (parseErrorCount > 0 || warningCount > 0) {
            return ImportBatchStatus.COMPLETED_WITH_WARNINGS;
        }
        return ImportBatchStatus.COMPLETED;
    }

    private Category getOrCreateImportCategory(CategoryType type, String categoryName) {
        return categoryRepository.findByTypeAndNameIgnoreCase(type, categoryName)
                .orElseGet(() -> {
                    Integer maxSort = categoryRepository.findMaxSortOrderByType(type);
                    int nextSort = (maxSort == null ? 0 : maxSort) + 1;
                    Category category = new Category(categoryName, type, nextSort, true);
                    return categoryRepository.save(category);
                });
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name is required");
        }
        return fileName.trim();
    }

    private String normalizeSourceName(String sourceName) {
        if (sourceName == null || sourceName.isBlank()) {
            return "Integration import";
        }
        return sourceName.trim();
    }

    private String normalizeDescription(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return "Imported transaction";
        }
        return normalized.length() > 300 ? normalized.substring(0, 300) : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String buildFingerprint(LocalDate date, TransactionType type, BigDecimal amount, String description) {
        String source = date + "|" + type + "|" + amount.setScale(2, RoundingMode.HALF_UP) + "|" + normalizeForFingerprint(description);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : bytes) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalizeForFingerprint(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private record CandidateTransaction(
            Integer rowNumber,
            LocalDate date,
            TransactionType type,
            BigDecimal amount,
            String description,
            String externalId,
            String sourceCategory,
            String fingerprint
    ) {
    }
}
