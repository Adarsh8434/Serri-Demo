package com.serri.api.DTO;


import lombok.Data;
import java.util.Map;

@Data
public class ContactDTO {

    // Core fields
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String mobile;
    private String companyName;
    private String designation;   // role
    private String pan;
    private String gender;
    private String dob;
    private String state;
    private String city;
    private String pincode;
    private String applicationStatus;
    private String leadId;

    /**
     * Arbitrary key-value attributes.
     * Example: { "industry": "FinTech", "fundingStage": "Series A" }
     * The client sends a plain Map — the service converts
     * each entry into a ContactAttribute entity.
     */
    private Map<String, String> attributes;
}
