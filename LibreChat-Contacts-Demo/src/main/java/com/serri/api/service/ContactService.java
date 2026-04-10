package com.serri.api.service;

import java.util.*;

import org.springframework.stereotype.Service;

import com.serri.api.DTO.ContactDTO;
import com.serri.api.model.Contact;
import com.serri.api.model.ContactAttribute;
import com.serri.api.repository.ContactRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    public Contact createFromDTO(ContactDTO dto) {
        Contact c = mapDtoToEntity(new Contact(), dto);
        return contactRepository.save(c);
    }

    public Optional<Contact> updateFromDTO(String id, ContactDTO dto) {
        return contactRepository.findById(id).map(existing -> {
            mapDtoToEntity(existing, dto);
            return contactRepository.save(existing);
        });
    }

    private Contact mapDtoToEntity(Contact c, ContactDTO dto) {
        c.setFirstName(dto.getFirstName());
        c.setMiddleName(dto.getMiddleName());
        c.setLastName(dto.getLastName());
        c.setEmail(dto.getEmail());
        c.setMobile(dto.getMobile());
        c.setCompanyName(dto.getCompanyName());
        c.setDesignation(dto.getDesignation());
        c.setPan(dto.getPan());
        c.setGender(dto.getGender());
        c.setDob(dto.getDob());
        c.setState(dto.getState());
        c.setCity(dto.getCity());
        c.setPincode(dto.getPincode());
        c.setApplicationStatus(dto.getApplicationStatus());
        c.setLeadId(dto.getLeadId());

        if (dto.getAttributes() != null) {
            c.getAttributes().clear();
            dto.getAttributes().forEach((key, value) -> {
                ContactAttribute attr = new ContactAttribute();
                attr.setContact(c);
                attr.setKey(key);
                attr.setValue(value);
                c.getAttributes().add(attr);
            });
        }
        return c;
    }
}
