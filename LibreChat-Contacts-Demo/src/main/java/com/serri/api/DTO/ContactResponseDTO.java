package com.serri.api.DTO;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import com.serri.api.model.Contact;
import com.serri.api.model.ContactAttribute;

@Data
public class ContactResponseDTO {

    private String id;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String mobile;
    private String companyName;
    private String designation;
    private String gender;
    private String dob;
    private String state;
    private String city;
    private String pincode;
    private String applicationStatus;
    private LocalDateTime createdAt;

    /**
     * All arbitrary attributes flattened into a simple map.
     * Example: { "industry": "AI", "location": "Bangalore" }
     */
    private Map<String, String> attributes;

    // Static factory — converts entity → DTO
    public static ContactResponseDTO from(Contact contact) {
        ContactResponseDTO dto = new ContactResponseDTO();
        dto.setId(contact.getId());
        dto.setFullName(contact.getFullName());
        dto.setFirstName(contact.getFirstName());
        dto.setMiddleName(contact.getMiddleName());
        dto.setLastName(contact.getLastName());
        dto.setEmail(contact.getEmail());
        dto.setMobile(contact.getMobile());
        dto.setCompanyName(contact.getCompanyName());
        dto.setDesignation(contact.getDesignation());
        dto.setGender(contact.getGender());
        dto.setDob(contact.getDob());
        dto.setState(contact.getState());
        dto.setCity(contact.getCity());
        dto.setPincode(contact.getPincode());
        dto.setApplicationStatus(contact.getApplicationStatus());
        dto.setCreatedAt(contact.getCreatedAt());

        // Flatten ContactAttribute list → Map<String, String>
        dto.setAttributes(
            contact.getAttributes().stream()
                .collect(Collectors.toMap(
                    ContactAttribute::getKey,
                    ContactAttribute::getValue,
                    (a, b) -> b  // keep last if duplicate keys
                ))
        );
        return dto;
    }
}
