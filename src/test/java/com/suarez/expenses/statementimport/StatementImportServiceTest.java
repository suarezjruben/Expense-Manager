package com.suarez.expenses.statementimport;

import com.suarez.expenses.account.Account;
import com.suarez.expenses.account.AccountService;
import com.suarez.expenses.transaction.BudgetTransaction;
import com.suarez.expenses.transaction.BudgetTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StatementImportServiceTest {

    @Autowired
    private StatementImportService statementImportService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private BudgetTransactionRepository budgetTransactionRepository;

    @Test
    void shouldImportCsvAndSkipDuplicates() {
        Account account = accountService.getOrCreateDefault();
        String csv = """
                date,amount,description,category,id
                2026-01-05,-10.00,Coffee,Food & Drink,tx-1
                2026-01-05,-10.00,Coffee,Food & Drink,tx-1
                2026-01-06,1000.00,Salary,Paycheck,tx-2
                2026-01-07,0.00,Zero row,Ignored,tx-3
                not-a-date,12.00,Broken,Ignored,tx-4
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        StatementImportResponseDto firstResponse = statementImportService.importStatement(
                account.getId(),
                file,
                null,
                null,
                null,
                null,
                null,
                false
        );
        assertThat(firstResponse.status()).isEqualTo(StatementImportResponseStatus.COMPLETED);
        assertThat(firstResponse.summary()).isNotNull();
        ImportSummaryDto firstImport = firstResponse.summary();

        assertThat(firstImport.inserted()).isEqualTo(2);
        assertThat(firstImport.skippedDuplicates()).isEqualTo(1);
        assertThat(firstImport.parseErrors()).hasSize(1);
        assertThat(firstImport.warnings()).hasSize(1);
        assertThat(firstImport.importBatchId()).isNotNull();

        assertThat(budgetTransactionRepository.findByAccountIdAndTxnDateBetween(
                account.getId(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        )).hasSize(2)
                .extracting(transaction -> transaction.getCategory().getName())
                .contains("Food & Drink", "Paycheck");

        MockMultipartFile fileAgain = new MockMultipartFile(
                "file",
                "statement.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        StatementImportResponseDto secondResponse = statementImportService.importStatement(
                account.getId(),
                fileAgain,
                null,
                null,
                null,
                null,
                null,
                false
        );
        assertThat(secondResponse.status()).isEqualTo(StatementImportResponseStatus.COMPLETED);
        assertThat(secondResponse.summary()).isNotNull();
        ImportSummaryDto secondImport = secondResponse.summary();

        assertThat(secondImport.inserted()).isEqualTo(0);
        assertThat(secondImport.skippedDuplicates()).isEqualTo(3);
        assertThat(secondImport.parseErrors()).hasSize(1);
        assertThat(secondImport.warnings()).hasSize(1);
    }

    @Test
    void shouldReturnHeaderMappingPromptForCsvWithoutHeader() {
        Account account = accountService.getOrCreateDefault();
        String csv = """
                "01/30/2026","-2310.00","*","","ZELLE TO JOSE TRAILAS ON 01/30 REF # WFCT0ZR9N94D"
                "01/30/2026","700.00","*","","ZELLE FROM DE PAZ JAIMEZ DIEGO ON 01/30 REF # WFCT0ZR8LFSX"
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Checking2.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        StatementImportResponseDto response = statementImportService.importStatement(
                account.getId(),
                file,
                null,
                null,
                null,
                null,
                null,
                false
        );

        assertThat(response.status()).isEqualTo(StatementImportResponseStatus.HEADER_MAPPING_REQUIRED);
        assertThat(response.summary()).isNull();
        assertThat(response.headerMappingPrompt()).isNotNull();
        assertThat(response.headerMappingPrompt().sampleRow()).hasSize(5);
    }

    @Test
    void shouldImportHeaderlessCsvUsingProvidedAndSavedMapping() {
        Account account = accountService.getOrCreateDefault();
        String csv = """
                "01/30/2026","-2310.00","*","","ZELLE TO JOSE TRAILAS ON 01/30 REF # WFCT0ZR9N94D"
                "01/30/2026","700.00","*","","ZELLE FROM DE PAZ JAIMEZ DIEGO ON 01/30 REF # WFCT0ZR8LFSX"
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Checking2.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        StatementImportResponseDto firstResponse = statementImportService.importStatement(
                account.getId(),
                file,
                0,
                1,
                4,
                null,
                null,
                true
        );
        assertThat(firstResponse.status()).isEqualTo(StatementImportResponseStatus.COMPLETED);
        assertThat(firstResponse.summary()).isNotNull();
        assertThat(firstResponse.summary().inserted()).isEqualTo(2);

        MockMultipartFile fileAgain = new MockMultipartFile(
                "file",
                "Checking2.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        StatementImportResponseDto secondResponse = statementImportService.importStatement(
                account.getId(),
                fileAgain,
                null,
                null,
                null,
                null,
                null,
                false
        );
        assertThat(secondResponse.status()).isEqualTo(StatementImportResponseStatus.COMPLETED);
        assertThat(secondResponse.summary()).isNotNull();
        assertThat(secondResponse.summary().inserted()).isEqualTo(0);
        assertThat(secondResponse.summary().skippedDuplicates()).isEqualTo(2);
    }

    @Test
    void shouldPreferMemoOverDescriptionWhenMemoIsPresent() {
        Account account = accountService.getOrCreateDefault();
        String csv = """
                Transaction Date,Post Date,Description,Category,Type,Amount,Memo
                01/29/2026,01/30/2026,Description should be ignored,Shopping,Sale,-21.28,Memo wins
                01/28/2026,01/29/2026,Fallback description,Bills & Utilities,Sale,-3.19,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        StatementImportResponseDto response = statementImportService.importStatement(
                account.getId(),
                file,
                null,
                null,
                null,
                null,
                null,
                false
        );

        assertThat(response.status()).isEqualTo(StatementImportResponseStatus.COMPLETED);
        assertThat(response.summary()).isNotNull();
        assertThat(response.summary().inserted()).isEqualTo(2);

        assertThat(budgetTransactionRepository.findByAccountIdAndTxnDateBetween(
                account.getId(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        )).extracting(BudgetTransaction::getDescription)
                .contains("Memo wins", "Fallback description")
                .doesNotContain("Description should be ignored");
    }
}
