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
}
