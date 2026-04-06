package com.serri.api.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.serri.api.model.Contact;
import com.serri.api.repository.ContactRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContactSearchService {

    private final ContactRepository contactRepository;

    private static final int MAX_CONTEXT_CONTACTS = 20;

   
    public List<Contact> findRelevantContacts(String userQuery) {

    String normalized = userQuery
            .toLowerCase()
            .replaceAll("[^a-z0-9 ]", " ")
            .trim();

    Set<String> stopWords = Set.of(
            "who","is","are","the","from","what","do","we","know",
            "about","list","all","in","our","show","me"
    );

    List<String> keywords = Arrays.stream(normalized.split("\\s+"))
            .filter(word -> !stopWords.contains(word))
            .toList();

    List<Contact> results = keywords.stream()
            .flatMap(k -> contactRepository
                    .searchContacts(k, PageRequest.of(0, MAX_CONTEXT_CONTACTS))
                    .getContent()
                    .stream())
            .distinct()
            .limit(MAX_CONTEXT_CONTACTS)
            .toList();

    return results;
}
//     public List<Contact> findRelevantContacts(String userQuery) {
//         String q = userQuery.toLowerCase();

//         // Try company match first
//         if (q.contains("work") || q.contains("at ") || q.contains("from ")) {
//             // Extract company name heuristic
//             String company = extractEntityAfter(q, List.of("at ", "from ", "work at ", "works at "));
//             if (company != null) {
//                 List<Contact> results = contactRepository
//                     .findByCompanyNameContainingIgnoreCase(company);
//                 if (!results.isEmpty()) return results.stream().limit(MAX_CONTEXT_CONTACTS).toList();
//             }
//         }

//         // Try role/designation match
//         if (q.contains("cto") || q.contains("ceo") || q.contains("manager") || q.contains("engineer")) {
//             String role = extractRole(q);
//             if (role != null) {
//                 return contactRepository.findByDesignationContainingIgnoreCase(role)
//                     .stream().limit(MAX_CONTEXT_CONTACTS).toList();
//             }
//         }

//         // Generic keyword search across all fields
//        Page<Contact> page = contactRepository.searchContacts(
//         q, PageRequest.of(0, MAX_CONTEXT_CONTACTS));
// return page.getContent();
//     }

    // private String extractEntityAfter(String text, List<String> triggers) {
    //     for (String trigger : triggers) {
    //         int idx = text.indexOf(trigger);
    //         if (idx >= 0) {
    //             String after = text.substring(idx + trigger.length()).trim();
    //             String[] words = after.split("\\s+");
    //             if (words.length > 0) return words[0]; // take first word as entity
    //         }
    //     }
    //     return null;
    // }

    // private String extractRole(String q) {
    //     for (String role : List.of("cto", "ceo", "coo", "manager", "engineer", "developer", "analyst")) {
    //         if (q.contains(role)) return role;
    //     }
    //     return null;
    // }
}
