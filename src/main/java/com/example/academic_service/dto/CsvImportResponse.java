package com.example.academic_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsvImportResponse {
    private boolean ok;
    private int createdCount;
    private List<RowError> errors = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        /** 1-based row number from the CSV (header counts as row 1). */
        private int row;
        private String message;
    }
}
