package com.serri.api.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serri.api.DTO.ContactDTO;
import com.serri.api.DTO.ContactResponseDTO;
import com.serri.api.DTO.PageContactResponse;
import com.serri.api.model.Contact;
import com.serri.api.repository.ContactRepository;
import com.serri.api.service.ContactService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final ContactRepository contactRepository;
    @GetMapping
    @Transactional(readOnly = true) 
    public PageContactResponse list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Contact> result = q.isBlank()
            ? contactRepository.findAll(PageRequest.of(page, size))
            : contactRepository.searchContacts(q, PageRequest.of(page, size));

        List<ContactResponseDTO> dtos = result.getContent()
            .stream().map(ContactResponseDTO::from).toList();

        return new PageContactResponse(
            dtos,
            result.getNumber(),
            result.getTotalPages(),
            result.getTotalElements()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponseDTO> get(@PathVariable String id) {
        return contactRepository.findById(id)
            .map(c -> ResponseEntity.ok(ContactResponseDTO.from(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ContactResponseDTO> create(@RequestBody ContactDTO dto) {
        Contact saved = contactService.createFromDTO(dto);
        return ResponseEntity.status(201).body(ContactResponseDTO.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponseDTO> update(
            @PathVariable String id, @RequestBody ContactDTO dto) {
        return contactService.updateFromDTO(id, dto)
            .map(c -> ResponseEntity.ok(ContactResponseDTO.from(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        contactRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 @GetMapping("/search")
@Transactional(readOnly = true)
public ResponseEntity<PageContactResponse> search(
        @RequestParam(required = false, defaultValue = "") String q,
        @RequestParam(defaultValue = "10") int size) {

    Page<Contact> result;

    if (q.isBlank()) {
        result = contactRepository.findAll(PageRequest.of(0, size));
    } else {
        result = contactRepository
            .searchContacts(q.trim(), PageRequest.of(0, size));
    }

    List<ContactResponseDTO> dtos = result.getContent()
        .stream()
        .map(ContactResponseDTO::from)
        .toList();

    return ResponseEntity.ok(new PageContactResponse(
        dtos, 0,
        result.getTotalPages(),
        result.getTotalElements()
    ));
}
@GetMapping("/search-simple")
public List<Map<String, String>> searchSimple(
        @RequestParam(required = false, defaultValue = "") String q) {

    List<Contact> contacts;

    if (q.isBlank()) {
        contacts = contactRepository.findAll(PageRequest.of(0, 10)).getContent();
    } else {
        contacts = contactRepository.searchContacts(q, PageRequest.of(0, 10)).getContent();
    }

    return contacts.stream().map(c -> Map.of(
        "fullName", c.getFullName(),
        "email", c.getEmail(),
        "city", c.getCity(),
        "state", c.getState(),
        "company", c.getCompanyName(),
        "role", c.getDesignation()
    )).toList();
}
@GetMapping(value = "/api/openapi", produces = "text/plain")
public ResponseEntity<String> openApiSpec() {
    String yaml = "openapi: 3.0.1\n" +
        "info:\n" +
        "  title: Contacts Workspace API\n" +
        "  version: 1.0.0\n" +
        "servers:\n" +
        "  - url: http://host.docker.internal:8080\n" +
        "paths:\n" +
        "  /api/contacts/search-simple:\n" +
        "    get:\n" +
        "      operationId: searchContacts\n" +
        "      summary: Search contacts by keyword\n" +
        "      description: Search contacts by name, company, city, state, role or email. Use this whenever the user asks about people contacts or companies.\n" +
        "      parameters:\n" +
        "        - name: q\n" +
        "          in: query\n" +
        "          required: false\n" +
        "          schema:\n" +
        "            type: string\n" +
        "      responses:\n" +
        "        '200':\n" +
        "          description: List of contacts\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: array\n" +
        "                items:\n" +
        "                  type: object\n" +
        "                  properties:\n" +
        "                    fullName:\n" +
        "                      type: string\n" +
        "                    email:\n" +
        "                      type: string\n" +
        "                    city:\n" +
        "                      type: string\n" +
        "                    state:\n" +
        "                      type: string\n" +
        "                    company:\n" +
        "                      type: string\n" +
        "                    role:\n" +
        "                      type: string\n";
    return ResponseEntity.ok(yaml);
}
@GetMapping("/context")
@Transactional(readOnly = true)
public ResponseEntity<Map<String, String>> getContext(
        @RequestParam(defaultValue = "") String q) {

    List<Contact> contacts;

    if (q.isBlank()) {
        contacts = contactRepository
            .findAll(PageRequest.of(0, 20))
            .getContent();
    } else {
        contacts = contactRepository
            .searchContacts(q, PageRequest.of(0, 15))
            .getContent();
    }

    String context = contacts.stream()
        .map(Contact::toContextString)
        .collect(Collectors.joining("\n---\n"));

    String systemPrompt = context.isBlank()
        ? "No contacts found."
        : "You are a contacts assistant. Use this data to answer:\n\n" + context;

    return ResponseEntity.ok(Map.of("systemPrompt", systemPrompt));
}
}