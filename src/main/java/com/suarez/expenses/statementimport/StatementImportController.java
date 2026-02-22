package com.suarez.expenses.statementimport;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/accounts/{accountId}/statement-imports")
public class StatementImportController {

    private final StatementImportService statementImportService;

    public StatementImportController(StatementImportService statementImportService) {
        this.statementImportService = statementImportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StatementImportResponseDto importStatement(
            @PathVariable Long accountId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Integer dateColumnIndex,
            @RequestParam(required = false) Integer amountColumnIndex,
            @RequestParam(required = false) Integer descriptionColumnIndex,
            @RequestParam(required = false) Integer categoryColumnIndex,
            @RequestParam(required = false) Integer externalIdColumnIndex,
            @RequestParam(defaultValue = "false") boolean saveHeaderMapping
    ) {
        return statementImportService.importStatement(
                accountId,
                file,
                dateColumnIndex,
                amountColumnIndex,
                descriptionColumnIndex,
                categoryColumnIndex,
                externalIdColumnIndex,
                saveHeaderMapping
        );
    }
}
