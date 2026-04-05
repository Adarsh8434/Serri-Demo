package com.serri.api.controller;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.serri.api.DTO.ImportResult;
import com.serri.api.service.CSVImportService;

import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Slf4j
public class CsvImportController {

    private final CSVImportService csvImportService;

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Only CSV files are allowed"));
        }

        try {
            log.info("Starting import of file: {} ({} bytes)",
                filename, file.getSize());

            ImportResult result = csvImportService.importCsv(file.getInputStream());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Import failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Import failed: " + e.getMessage(),
                    "totalImported", 0
                ));
        }
    }
}