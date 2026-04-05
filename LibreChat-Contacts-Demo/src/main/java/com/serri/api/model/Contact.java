package com.serri.api.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contact_email",    columnList = "email"),
    @Index(name = "idx_contact_company",  columnList = "company_name"),
    @Index(name = "idx_contact_state",    columnList = "state"),
    @Index(name = "idx_contact_city",     columnList = "city"),
    @Index(name = "idx_contact_status",   columnList = "application_status")
})
@Data
@NoArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ── Core identity fields ──────────────────────────────
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String mobile;
    private String pan;
    private String gender;
    private String dob;

    // ── Location ──────────────────────────────────────────
    private String city;
    private String state;
    private String pincode;

    // ── Professional ─────────────────────────────────────
    private String companyName;
    private String designation;

    // ── Application tracking ─────────────────────────────
    private String applicationStatus;
    private String leadId;
    private String chatId;
    private String stateId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // ── Arbitrary attributes (kyc, loan, lat/lng, etc.) ──
    @OneToMany(mappedBy = "contact",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    private List<ContactAttribute> attributes = new ArrayList<>();

    // ── Helpers ───────────────────────────────────────────
    public String getFullName() {
        return Stream.of(firstName, middleName, lastName)
                     .filter(s -> s != null && !s.isBlank())
                     .collect(Collectors.joining(" "));
    }

    /** Used for AI context injection */
    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getFullName()).append("\n");
        if (email != null && !email.isBlank())
            sb.append("Email: ").append(email).append("\n");
        if (mobile != null && !mobile.isBlank())
            sb.append("Mobile: ").append(mobile).append("\n");
        if (companyName != null && !companyName.isBlank())
            sb.append("Company: ").append(companyName).append("\n");
        if (designation != null && !designation.isBlank())
            sb.append("Role: ").append(designation).append("\n");
        if (city != null && !city.isBlank())
            sb.append("City: ").append(city).append("\n");
        if (state != null && !state.isBlank())
            sb.append("State: ").append(state).append("\n");
        if (applicationStatus != null && !applicationStatus.isBlank())
            sb.append("Status: ").append(applicationStatus).append("\n");
        for (ContactAttribute attr : attributes) {
            if (attr.getValue() != null && !attr.getValue().isBlank())
                sb.append(attr.getKey()).append(": ").append(attr.getValue()).append("\n");
        }
        return sb.toString();
    }
}