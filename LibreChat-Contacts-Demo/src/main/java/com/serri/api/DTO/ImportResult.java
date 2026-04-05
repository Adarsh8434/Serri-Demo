package com.serri.api.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportResult {
    private int totalImported;
    private int totalSkipped;
    private String status;

    public String getMessage() {
        return String.format("%d contacts imported. %d rows skipped.",
            totalImported, totalSkipped);
    }
}