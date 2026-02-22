package com.suarez.expenses.statementimport;

public class CsvHeaderMappingRequiredException extends RuntimeException {

    private final CsvHeaderMappingPromptDto prompt;

    public CsvHeaderMappingRequiredException(CsvHeaderMappingPromptDto prompt) {
        super(prompt == null ? "CSV header mapping is required" : prompt.message());
        this.prompt = prompt;
    }

    public CsvHeaderMappingPromptDto getPrompt() {
        return prompt;
    }
}
