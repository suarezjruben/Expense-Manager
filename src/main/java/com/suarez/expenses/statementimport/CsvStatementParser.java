package com.suarez.expenses.statementimport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.IntStream;

@Component
public class CsvStatementParser implements StatementParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private static final Set<String> DATE_HEADERS = Set.of(
            "date", "txn date", "transaction date", "posted date", "post date"
    );
    private static final Set<String> AMOUNT_HEADERS = Set.of(
            "amount", "transaction amount", "amt"
    );
    private static final Set<String> DEBIT_HEADERS = Set.of(
            "debit", "withdrawal", "outflow", "money out"
    );
    private static final Set<String> CREDIT_HEADERS = Set.of(
            "credit", "deposit", "inflow", "money in"
    );
    private static final Set<String> MEMO_HEADERS = Set.of(
            "memo"
    );
    private static final Set<String> DESCRIPTION_HEADERS = Set.of(
            "description", "payee", "name", "details"
    );
    private static final Set<String> CATEGORY_HEADERS = Set.of(
            "category", "category name", "classification"
    );
    private static final Set<String> EXTERNAL_ID_HEADERS = Set.of(
            "fitid", "id", "transaction id", "reference", "reference id"
    );
    private static final Set<String> ALL_KNOWN_HEADERS = new HashSet<>();

    static {
        ALL_KNOWN_HEADERS.addAll(DATE_HEADERS);
        ALL_KNOWN_HEADERS.addAll(AMOUNT_HEADERS);
        ALL_KNOWN_HEADERS.addAll(DEBIT_HEADERS);
        ALL_KNOWN_HEADERS.addAll(CREDIT_HEADERS);
        ALL_KNOWN_HEADERS.addAll(MEMO_HEADERS);
        ALL_KNOWN_HEADERS.addAll(DESCRIPTION_HEADERS);
        ALL_KNOWN_HEADERS.addAll(CATEGORY_HEADERS);
        ALL_KNOWN_HEADERS.addAll(EXTERNAL_ID_HEADERS);
    }

    @Override
    public boolean supports(StatementFileType fileType) {
        return fileType == StatementFileType.CSV;
    }

    @Override
    public StatementParseResult parse(InputStream inputStream) throws IOException {
        return parse(inputStream, null);
    }

    public StatementParseResult parse(InputStream inputStream, CsvColumnMapping csvColumnMapping) throws IOException {
        List<NormalizedStatementRow> rows = new ArrayList<>();
        List<StatementIssue> issues = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                issues.add(StatementIssue.error(null, "CSV is empty"));
                return new StatementParseResult(List.of(), issues);
            }

            CSVRecord firstRecord = records.getFirst();
            if (looksLikeHeader(firstRecord)) {
                parseWithHeader(records, rows, issues);
            } else {
                if (csvColumnMapping == null) {
                    throw buildHeaderMappingRequired(firstRecord);
                }
                parseWithoutHeader(records, csvColumnMapping, rows, issues);
            }
        }

        return new StatementParseResult(rows, issues);
    }

    private void parseWithHeader(List<CSVRecord> records, List<NormalizedStatementRow> rows, List<StatementIssue> issues) {
        CSVRecord headerRecord = records.getFirst();
        Map<String, Integer> headerIndexByName = indexHeaderColumns(headerRecord);
        Integer dateIndex = findColumnIndex(headerIndexByName, DATE_HEADERS);
        Integer amountIndex = findColumnIndex(headerIndexByName, AMOUNT_HEADERS);
        Integer debitIndex = findColumnIndex(headerIndexByName, DEBIT_HEADERS);
        Integer creditIndex = findColumnIndex(headerIndexByName, CREDIT_HEADERS);
        Integer memoIndex = findColumnIndex(headerIndexByName, MEMO_HEADERS);
        Integer descriptionIndex = findColumnIndex(headerIndexByName, DESCRIPTION_HEADERS);
        Integer categoryIndex = findColumnIndex(headerIndexByName, CATEGORY_HEADERS);
        Integer externalIdIndex = findColumnIndex(headerIndexByName, EXTERNAL_ID_HEADERS);

        if (dateIndex == null) {
            issues.add(StatementIssue.error(null, "CSV is missing a date column"));
            return;
        }
        if (amountIndex == null && debitIndex == null && creditIndex == null) {
            issues.add(StatementIssue.error(null, "CSV is missing amount or debit/credit columns"));
            return;
        }

        for (int i = 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            parseRecord(
                    record,
                    dateIndex,
                    amountIndex,
                    debitIndex,
                    creditIndex,
                    memoIndex,
                    descriptionIndex,
                    categoryIndex,
                    externalIdIndex,
                    rows,
                    issues
            );
        }
    }

    private void parseWithoutHeader(
            List<CSVRecord> records,
            CsvColumnMapping mapping,
            List<NormalizedStatementRow> rows,
            List<StatementIssue> issues
    ) {
        for (CSVRecord record : records) {
            parseRecord(
                    record,
                    mapping.dateColumnIndex(),
                    mapping.amountColumnIndex(),
                    null,
                    null,
                    null,
                    mapping.descriptionColumnIndex(),
                    mapping.categoryColumnIndex(),
                    mapping.externalIdColumnIndex(),
                    rows,
                    issues
            );
        }
    }

    private void parseRecord(
            CSVRecord record,
            Integer dateIndex,
            Integer amountIndex,
            Integer debitIndex,
            Integer creditIndex,
            Integer memoIndex,
            Integer descriptionIndex,
            Integer categoryIndex,
            Integer externalIdIndex,
            List<NormalizedStatementRow> rows,
            List<StatementIssue> issues
    ) {
        Integer rowNumber = safeRowNumber(record);
        String dateRaw = get(record, dateIndex);
        String amountRaw = get(record, amountIndex);
        String debitRaw = get(record, debitIndex);
        String creditRaw = get(record, creditIndex);
        String memoRaw = normalize(get(record, memoIndex));
        String descriptionRaw = normalize(get(record, descriptionIndex));
        String categoryRaw = get(record, categoryIndex);
        String externalIdRaw = get(record, externalIdIndex);

        LocalDate date = parseDate(dateRaw);
        if (date == null) {
            issues.add(StatementIssue.error(rowNumber, "Invalid or empty date"));
            return;
        }

        BigDecimal signedAmount = resolveAmount(amountRaw, debitRaw, creditRaw);
        if (signedAmount == null) {
            issues.add(StatementIssue.error(rowNumber, "Invalid or empty amount"));
            return;
        }

        String description = memoRaw != null ? memoRaw : descriptionRaw;
        if (description == null) {
            description = "Imported transaction";
            issues.add(StatementIssue.warning(rowNumber, "Missing description. Defaulted to Imported transaction"));
        }

        rows.add(new NormalizedStatementRow(
                rowNumber,
                date,
                signedAmount,
                description,
                normalize(externalIdRaw),
                normalize(categoryRaw)
        ));
    }

    private boolean looksLikeHeader(CSVRecord firstRecord) {
        if (firstRecord == null || firstRecord.size() == 0) {
            return false;
        }
        String firstCell = normalize(firstRecord.get(0));
        String secondCell = firstRecord.size() > 1 ? normalize(firstRecord.get(1)) : null;
        if (parseDate(firstCell) != null && parseAmount(secondCell) != null) {
            return false;
        }

        long knownHeaderCellCount = IntStream.range(0, firstRecord.size())
                .mapToObj(firstRecord::get)
                .map(this::normalizeHeader)
                .filter(ALL_KNOWN_HEADERS::contains)
                .count();
        return knownHeaderCellCount >= 2;
    }

    private CsvHeaderMappingRequiredException buildHeaderMappingRequired(CSVRecord firstRecord) {
        List<String> sampleRow = new ArrayList<>();
        for (int i = 0; i < firstRecord.size(); i++) {
            sampleRow.add(normalize(firstRecord.get(i)));
        }

        CsvColumnMapping inferred = inferColumnMapping(firstRecord);
        CsvHeaderMappingPromptDto prompt = new CsvHeaderMappingPromptDto(
                "CSV file has no recognizable header row. Provide column indexes to continue import.",
                firstRecord.size(),
                sampleRow,
                inferred == null ? null : inferred.dateColumnIndex(),
                inferred == null ? null : inferred.amountColumnIndex(),
                inferred == null ? null : inferred.descriptionColumnIndex(),
                inferred == null ? null : inferred.categoryColumnIndex(),
                inferred == null ? null : inferred.externalIdColumnIndex()
        );
        return new CsvHeaderMappingRequiredException(prompt);
    }

    private CsvColumnMapping inferColumnMapping(CSVRecord record) {
        Integer dateIndex = null;
        Integer amountIndex = null;
        Integer descriptionIndex = null;

        for (int i = 0; i < record.size(); i++) {
            if (dateIndex == null && parseDate(record.get(i)) != null) {
                dateIndex = i;
            }
        }

        for (int i = 0; i < record.size(); i++) {
            if (Objects.equals(i, dateIndex)) {
                continue;
            }
            if (amountIndex == null && parseAmount(record.get(i)) != null) {
                amountIndex = i;
            }
        }

        int longest = -1;
        for (int i = 0; i < record.size(); i++) {
            if (Objects.equals(i, dateIndex) || Objects.equals(i, amountIndex)) {
                continue;
            }
            String value = normalize(record.get(i));
            if (value == null) {
                continue;
            }
            boolean containsLetter = value.chars().anyMatch(Character::isLetter);
            if (!containsLetter) {
                continue;
            }
            if (value.length() > longest) {
                longest = value.length();
                descriptionIndex = i;
            }
        }

        if (dateIndex == null || amountIndex == null || descriptionIndex == null) {
            return null;
        }
        return new CsvColumnMapping(dateIndex, amountIndex, descriptionIndex, null, null);
    }

    private Map<String, Integer> indexHeaderColumns(CSVRecord headerRecord) {
        Map<String, Integer> normalizedToIndex = new HashMap<>();
        for (int i = 0; i < headerRecord.size(); i++) {
            String rawHeader = headerRecord.get(i);
            if (rawHeader == null) {
                continue;
            }
            normalizedToIndex.putIfAbsent(normalizeHeader(rawHeader), i);
        }
        return normalizedToIndex;
    }

    private Integer findColumnIndex(Map<String, Integer> indexedHeaders, Set<String> candidates) {
        for (String candidate : candidates) {
            Integer match = indexedHeaders.get(normalizeHeader(candidate));
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private String get(CSVRecord record, Integer columnIndex) {
        if (columnIndex == null || columnIndex < 0 || columnIndex >= record.size()) {
            return null;
        }
        return record.get(columnIndex);
    }

    private Integer safeRowNumber(CSVRecord record) {
        try {
            return Math.toIntExact(record.getRecordNumber() + 1);
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String raw) {
        String value = normalize(raw);
        if (value == null) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private BigDecimal resolveAmount(String amountRaw, String debitRaw, String creditRaw) {
        BigDecimal amount = parseAmount(amountRaw);
        if (amount != null) {
            return amount;
        }

        BigDecimal debit = parseAmount(debitRaw);
        BigDecimal credit = parseAmount(creditRaw);
        if (debit == null && credit == null) {
            return null;
        }
        BigDecimal normalizedDebit = debit == null ? BigDecimal.ZERO : debit.abs();
        BigDecimal normalizedCredit = credit == null ? BigDecimal.ZERO : credit.abs();
        return normalizedCredit.subtract(normalizedDebit);
    }

    private BigDecimal parseAmount(String raw) {
        String value = normalize(raw);
        if (value == null) {
            return null;
        }

        boolean negative = false;
        if (value.startsWith("(") && value.endsWith(")")) {
            negative = true;
            value = value.substring(1, value.length() - 1);
        }
        if (value.endsWith("-")) {
            negative = true;
            value = value.substring(0, value.length() - 1);
        }

        String cleaned = value.replace(",", "")
                .replace("$", "")
                .replace(" ", "");
        if (cleaned.isBlank()) {
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(cleaned);
            return negative ? parsed.negate() : parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replace("_", " ").replaceAll("\\s+", " ").trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
