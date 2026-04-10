package com.serri.api.controller;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.serri.api.model.Contact;
import com.serri.api.service.ContactSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/librechat")
@RequiredArgsConstructor
@Slf4j
public class LibreChatProxyController {

    private final ContactSearchService contactSearchService;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL ="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> models() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", "contacts-gemini");
        model.put("object", "model");
        model.put("created", 1700000000);
        model.put("owned_by", "contacts-workspace");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("object", "list");
        response.put("data", List.of(model));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/completions/debug")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request) {

        log.info("=== Chat request received ===");
        log.info("Raw request keys: {}", request.keySet());

       
        String userMessage = "";
        try {
            Object messagesObj = request.get("messages");
            if (messagesObj instanceof List) {
                List<?> messagesList = (List<?>) messagesObj;
                for (int i = messagesList.size() - 1; i >= 0; i--) {
                    Object msgObj = messagesList.get(i);
                    if (msgObj instanceof Map) {
                        Map<?, ?> msg = (Map<?, ?>) msgObj;
                        Object role = msg.get("role");
                        if ("user".equals(role)) {
                            Object content = msg.get("content");
                            if (content instanceof String) {
                                userMessage = (String) content;
                            } else if (content instanceof List) {
                                List<?> parts = (List<?>) content;
                                StringBuilder sb = new StringBuilder();
                                for (Object part : parts) {
                                    if (part instanceof Map) {
                                        Object text = ((Map<?, ?>) part).get("text");
                                        if (text instanceof String) {
                                            sb.append(text);
                                        }
                                    }
                                }
                                userMessage = sb.toString();
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting user message: {}", e.getMessage());
        }

        log.info("User asked: {}", userMessage);

     
        List<Contact> contacts = new ArrayList<>();
        try {
            contacts = contactSearchService.findRelevantContacts(userMessage);
        } catch (Exception e) {
            log.error("Error finding contacts: {}", e.getMessage());
        }
        log.info("Contacts found: {}", contacts.size());

        String context = contacts.stream()
            .map(Contact::toContextString)
            .collect(Collectors.joining("\n---\n"));

        
        String fullPrompt = context.isBlank()
            ? "You are a contacts workspace assistant. "
              + "The user asked: \"" + userMessage + "\". "
              + "No matching contacts were found. Politely inform the user."
            : "You are a contacts workspace assistant.\n"
              + "Answer using ONLY the contact data below. Be concise.\n\n"
              + "CONTACTS:\n" + context + "\n\n"
              + "User Question: " + userMessage;

        // 5. Call Gemini
String answer = callGemini(fullPrompt);
boolean isStream = Boolean.TRUE.equals(request.get("stream"));
log.info("Stream mode: {}", isStream);
        // 6. Build OpenAI-compatible response
        return ResponseEntity.ok(buildResponse(answer));
    }
    
private Map<String, Object> buildResponse(String answer) {
    if (answer == null || answer.isBlank()) {
        answer = "I could not process your request.";
    }

   
    Map<String, Object> messageMap = new LinkedHashMap<>();
    messageMap.put("role", "assistant");
    messageMap.put("content", answer);

    Map<String, Object> choice = new LinkedHashMap<>();
    choice.put("index", 0);
    choice.put("message", messageMap);
    choice.put("finish_reason", "stop");

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", "chatcmpl-" + UUID.randomUUID());
    response.put("object", "chat.completion");
    response.put("created", System.currentTimeMillis() / 1000);
    response.put("model", "contacts-gemini"); 
    response.put("choices", Collections.singletonList(choice)); 
    
    Map<String, Object> usage = new HashMap<>();
    usage.put("prompt_tokens", 10);
    usage.put("completion_tokens", 10);
    usage.put("total_tokens", 20);
    response.put("usage", usage);

    return response;
}
 
    private String callGemini(String prompt) {
        try {
            Map<String, Object> part = new LinkedHashMap<>();
            part.put("text", prompt);

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("parts", List.of(part));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // String url = GEMINI_URL + "?key=" + geminiApiKey;
           String url = GEMINI_URL + "?key=" + geminiApiKey.trim();
            ResponseEntity<Map> resp = new RestTemplate().postForEntity(
                url,
                new HttpEntity<>(body, headers),
                Map.class
            );

            if (resp.getBody() == null) {
                return "No response from Gemini";
            }

            Object candidatesObj = resp.getBody().get("candidates");
            if (!(candidatesObj instanceof List)) {
                return "Invalid response from Gemini";
            }

            List<?> candidates = (List<?>) candidatesObj;
            if (candidates.isEmpty()) {
                return "Empty response from Gemini";
            }

            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> contentMap = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) contentMap.get("parts");
            String result = (String) ((Map<?, ?>) parts.get(0)).get("text");

            log.info("Gemini responded successfully"+result);
            return result;

        } catch (Exception e) {
            log.error("Gemini failed: {}", e.getMessage(), e);
            return "Error calling AI: " + e.getMessage();
        }
    }
    @PostMapping("/chat/completions/1")
public ResponseEntity<Map<String, Object>> debug(
        @RequestBody Map<String, Object> request) {
    log.info("FULL REQUEST: {}", request);

    Map<String, Object> msg = new LinkedHashMap<>();
    msg.put("role", "assistant");
    msg.put("content", "test response");

    Map<String, Object> choice = new LinkedHashMap<>();
    choice.put("index", 0);
    choice.put("message", msg);
    choice.put("finish_reason", "stop");

    Map<String, Object> resp = new LinkedHashMap<>();
    resp.put("id", "test-123");
    resp.put("object", "chat.completion");
    resp.put("created", 1234567890);
    resp.put("model", "contacts-gemini");
    resp.put("choices", List.of(choice));

    return ResponseEntity.ok(resp);
}
@PostMapping("/chat/completions")
@Transactional(readOnly = true)
public ResponseEntity<StreamingResponseBody> chat1(
        @RequestBody Map<String, Object> request) {

    log.info("FULL REQUEST: {}", request);

    String userMessage = extractUserMessage(request);
    log.info("User asked: {}", userMessage);

    List<Contact> contacts = new ArrayList<>();
    try {
        contacts = contactSearchService.findRelevantContacts(userMessage);
    } catch (Exception e) {
        log.error("Contact search error: {}", e.getMessage());
    }
    log.info("Contacts found: {}", contacts.size());

    String context = contacts.stream()
        .map(Contact::toContextString)
        .collect(Collectors.joining("\n---\n"));

    String prompt = context.isBlank()
        ? "You are a contacts assistant. No contacts found for: \""
          + userMessage + "\". Politely inform the user."
        : "You are a contacts workspace assistant.\n"
          + "Answer ONLY using the contact data below. Be concise.\n\n"
          + "CONTACTS:\n" + context + "\n\n"
          + "User Question: " + userMessage;

    // 4. Call Gemini
    String answer = callGemini(prompt);
    if (answer == null || answer.isBlank()) {
        answer = "I could not process your request.";
    }

    final String finalAnswer = answer;
    final String chatId = "chatcmpl-" + UUID.randomUUID();
    final long created = System.currentTimeMillis() / 1000;

    StreamingResponseBody stream = outputStream -> {
        try {
            
            String chunk1 = "{\"id\":\"" + chatId + "\","
                + "\"object\":\"chat.completion.chunk\","
                + "\"created\":" + created + ","
                + "\"model\":\"contacts-gemini\","
                + "\"choices\":[{\"index\":0,"
                + "\"delta\":{\"role\":\"assistant\",\"content\":\"\"},"
                + "\"finish_reason\":null}]}";
            outputStream.write(("data: " + chunk1 + "\n\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();

            
            String escapedAnswer = finalAnswer
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

            String chunk2 = "{\"id\":\"" + chatId + "\","
                + "\"object\":\"chat.completion.chunk\","
                + "\"created\":" + created + ","
                + "\"model\":\"contacts-gemini\","
                + "\"choices\":[{\"index\":0,"
                + "\"delta\":{\"content\":\"" + escapedAnswer + "\"},"
                + "\"finish_reason\":null}]}";
            outputStream.write(("data: " + chunk2 + "\n\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();

            
            String chunk3 = "{\"id\":\"" + chatId + "\","
                + "\"object\":\"chat.completion.chunk\","
                + "\"created\":" + created + ","
                + "\"model\":\"contacts-gemini\","
                + "\"choices\":[{\"index\":0,"
                + "\"delta\":{},"
                + "\"finish_reason\":\"stop\"}]}";
            outputStream.write(("data: " + chunk3 + "\n\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();

            // Done
            outputStream.write("data: [DONE]\n\n"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();

        } catch (Exception e) {
            log.error("Stream error: {}", e.getMessage());
        }
    };

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "text/event-stream")
        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
        .header(HttpHeaders.CONNECTION, "keep-alive")
        .body(stream);
}

 private String extractUserMessage(Map<String, Object> request) {
        try {
            Object messagesObj = request.get("messages");
            if (!(messagesObj instanceof List)) return "";
            List<?> list = (List<?>) messagesObj;
            for (int i = list.size() - 1; i >= 0; i--) {
                if (!(list.get(i) instanceof Map)) continue;
                Map<?, ?> msg = (Map<?, ?>) list.get(i);
                if (!"user".equals(msg.get("role"))) continue;
                Object content = msg.get("content");
                if (content instanceof String) return (String) content;
                if (content instanceof List) {
                    StringBuilder sb = new StringBuilder();
                    for (Object p : (List<?>) content) {
                        if (p instanceof Map) {
                            Object t = ((Map<?, ?>) p).get("text");
                            if (t instanceof String) sb.append(t);
                        }
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.error("Error extracting message: {}", e.getMessage());
        }
        return "";
    }
}


