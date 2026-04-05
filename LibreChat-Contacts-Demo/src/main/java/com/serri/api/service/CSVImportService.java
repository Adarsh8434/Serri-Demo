package com.serri.api.service;

import com.serri.api.DTO.ImportResult;
import com.serri.api.model.Contact;
import com.serri.api.model.ContactAttribute;
import com.serri.api.repository.ContactRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CSVImportService {
    private final ContactRepository contactRepository;
    private final BatchService batchSaveService; 
    private static final int BATCH_SIZE = 300;

    private static final Set<String> CORE_FIELDS = Set.of(
        "id", "chat_id", "state_id", "lead_id",
        "application_status", "pincode",
        "first_name", "middle_name", "last_name",
        "mobile", "email", "pan", "gender", "dob",
        "state", "city", "company_name", "designation"
    );

    // NOT @Transactional here — parsing is pure memory work
    public ImportResult importCsv(InputStream inputStream) throws Exception {
        List<Contact> allContacts = new ArrayList<>();
        int totalSkipped = 0;

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)).build()) {

            String[] headers = reader.readNext();
            if (headers == null) throw new RuntimeException("CSV is empty");

            // Clean headers (remove BOM, trim whitespace)
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim()
                                       .toLowerCase()
                                       .replace("\uFEFF", "");
            }
            log.info("CSV Headers found: {}", Arrays.toString(headers));

            String[] row;
            while ((row = reader.readNext()) != null) {
                try {
                    allContacts.add(mapRowToContact(headers, row));
                } catch (Exception e) {
                    totalSkipped++;
                    log.warn("Skipping bad row: {}", e.getMessage());
                }
            }
        }

        log.info("Parsed {} contacts, saving in batches of {}...",
            allContacts.size(), BATCH_SIZE);

        // Delegate to SEPARATE bean so @Transactional proxy fires correctly
        int saved = 0;
        for (int i = 0; i < allContacts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allContacts.size());
            saved += batchSaveService.saveBatch(allContacts.subList(i, end));
            log.info("Progress: {}/{}", saved, allContacts.size());
        }

        log.info("Import complete. Saved: {}, Skipped: {}", saved, totalSkipped);
        return new ImportResult(saved, totalSkipped, "Success");
    }

    private Contact mapRowToContact(String[] headers, String[] values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String val = (i < values.length) ? values[i].trim() : "";
            if (!val.isEmpty() && !val.equals("\"\"")) {
                row.put(headers[i], val);
            }
        }

        Contact c = new Contact();
        c.setFirstName(row.get("first_name"));
        c.setMiddleName(row.get("middle_name"));
        c.setLastName(row.get("last_name"));
        c.setEmail(row.get("email"));
        c.setMobile(row.get("mobile"));
        c.setPan(row.get("pan"));
        c.setGender(row.get("gender"));
        c.setDob(row.get("dob"));
        c.setCity(row.get("city"));
        c.setState(row.get("state"));
        c.setPincode(row.get("pincode"));
        c.setCompanyName(row.get("company_name"));
        c.setDesignation(row.get("designation"));
        c.setApplicationStatus(row.get("application_status"));
        c.setLeadId(row.get("lead_id"));
        c.setChatId(row.get("chat_id"));
        c.setStateId(row.get("state_id"));

        // Non-core fields → ContactAttribute
        List<ContactAttribute> attrs = new ArrayList<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (!CORE_FIELDS.contains(entry.getKey())) {
                ContactAttribute attr = new ContactAttribute();
                attr.setContact(c);
                attr.setKey(entry.getKey());
                attr.setValue(entry.getValue());
                attrs.add(attr);
            }
        }
        c.setAttributes(attrs);
        return c;
    }
}