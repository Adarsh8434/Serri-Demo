package com.serri.api.DTO;


import lombok.Data;
import java.util.Map;

@Data
public class ContactDTO {
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String mobile;
    private String companyName;
    private String designation;   
    private String pan;
    private String gender;
    private String dob;
    private String state;
    private String city;
    private String pincode;
    private String applicationStatus;
    private String leadId;

    private Map<String, String> attributes;
}
