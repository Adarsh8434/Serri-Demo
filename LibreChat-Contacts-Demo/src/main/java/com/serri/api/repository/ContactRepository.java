package com.serri.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.serri.api.model.Contact;

public interface ContactRepository extends JpaRepository<Contact, String> {

    // For smart filtering (extra credit)
    List<Contact> findByCompanyNameContainingIgnoreCase(String company);
    List<Contact> findByStateContainingIgnoreCase(String state);
    List<Contact> findByDesignationContainingIgnoreCase(String designation);

    // @Query("""
    //     SELECT c FROM Contact c WHERE
    //     LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR
    //     LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR
    //     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :q, '%')) OR
    //     LOWER(c.designation) LIKE LOWER(CONCAT('%', :q, '%')) OR
    //     LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%')) OR
    //     LOWER(c.applicationStatus) LIKE LOWER(CONCAT('%', :q, '%'))
    // """)
    // Page<Contact> searchContacts(@Param("q") String query, Pageable pageable);

    @Query("""
SELECT c FROM Contact c
WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
   OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :q, '%'))
   OR LOWER(c.designation) LIKE LOWER(CONCAT('%', :q, '%'))
   OR LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%'))
   OR LOWER(c.state) LIKE LOWER(CONCAT('%', :q, '%'))
""")
Page<Contact> searchContacts(@Param("q") String q, Pageable pageable);
}
