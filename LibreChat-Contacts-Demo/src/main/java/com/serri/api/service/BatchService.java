package com.serri.api.service;

import com.serri.api.model.Contact;
import com.serri.api.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final ContactRepository contactRepository;

    /**
     * Each call to this method runs in its own transaction.
     * Because this is a SEPARATE bean, Spring's proxy intercepts
     * @Transactional correctly — unlike self-calls within same class.
     */
    @Transactional
    public int saveBatch(List<Contact> batch) {
        contactRepository.saveAll(batch);
        return batch.size();
    }
}