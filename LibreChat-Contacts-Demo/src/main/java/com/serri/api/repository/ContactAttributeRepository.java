package com.serri.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.serri.api.model.ContactAttribute;

import java.util.List;

@Repository
public interface ContactAttributeRepository extends JpaRepository<ContactAttribute, String> {
    List<ContactAttribute> findByContactId(String contactId);
    void deleteByContactId(String contactId);
}