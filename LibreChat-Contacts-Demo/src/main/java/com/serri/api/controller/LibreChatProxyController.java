// package com.serri.api.controller;

// import com.serri.api.model.Contact;
// import com.serri.api.service.ContactSearchService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.*;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.client.RestTemplate;

// import java.util.*;
// import java.util.stream.Collectors;

// @RestController
// @Slf4j
// @RequiredArgsConstructor
// @RequestMapping("/librechat")
// public class LibreChatProxyController {

//     private final ContactSearchService contactSearchService;

//     @Value("${llm.api.key}")
//     private String apiKey;
//     @Value("${gemini.api.key}")
//     private String geminiApiKey;
//     @GetMapping("/models")
//     public ResponseEntity<Map<String, Object>> models() {
//         return ResponseEntity.ok(Map.of(
//             "object", "list",
//             "data", List.of(Map.of(
//                 "id", "contacts-gemini",
//                 "object", "model",
//                 "created", 1700000000,
//                 "owned_by", "contacts-workspace"
//             ))
//         ));
//     }

// @PostMapping("/chat/completions")
// @Transactional(readOnly = true)
// public ResponseEntity<Map<String, Object>> chat(
//         @RequestBody Map<String, Object> request) {

//     log.info("=== Chat request received ===");

//     List<Map<String, String>> messages =
//         (List<Map<String, String>>) request.get("messages");

//     // String userMessage = messages.stream()
//     //     .filter(m -> "user".equals(m.get("role")))
//     //     .map(m -> m.get("content"))
//     //     .reduce((a, b) -> b).orElse("");
//     String userMessage = messages.stream()
//     .filter(m -> "user".equals(m.get("role")))
//     .map(m -> {
//         Object content = m.get("content");

//         if (content instanceof String) {
//             return (String) content;
//         } else if (content instanceof List) {
//             List<?> parts = (List<?>) content;
//             return parts.stream()
//                 .map(p -> (Map<?, ?>) p)
//                 .map(p -> (String) p.get("text"))
//                 .collect(Collectors.joining(" "));
//         }
//         return "";
//     })
//     .reduce((a, b) -> b)
//     .orElse("");

//     log.info("User asked: {}", userMessage);

//     List<Contact> contacts =
//         contactSearchService.findRelevantContacts(userMessage);
//     log.info("Contacts found: {}", contacts.size());

//     String context = contacts.stream()
//         .map(Contact::toContextString)
//         .collect(Collectors.joining("\n---\n"));

//     String fullPrompt = context.isBlank()
//         ? "You are a contacts assistant. No contacts found. User asked: " + userMessage
//         : "You are a contacts assistant.\nAnswer using ONLY this data:\n\n"
//           + context + "\n\nUser Question: " + userMessage;

//     String answer = callGemini(fullPrompt);

//     // Build response exactly as LibreChat expects
//     Map<String, String> messageMap = new LinkedHashMap<>();
//     messageMap.put("role", "assistant");
//     messageMap.put("content", answer);

//     Map<String, Object> choice = new LinkedHashMap<>();
//     choice.put("index", 0);
//     choice.put("message", messageMap);
//     choice.put("finish_reason", "stop");

//     Map<String, Object> usage = new LinkedHashMap<>();
//     usage.put("prompt_tokens", 0);
//     usage.put("completion_tokens", 0);
//     usage.put("total_tokens", 0);

//     Map<String, Object> response = new LinkedHashMap<>();
//     response.put("id", "chatcmpl-" + UUID.randomUUID());
//     response.put("object", "chat.completion");
//     response.put("created", System.currentTimeMillis() / 1000);
//     response.put("model", "contacts-gemini");
//     response.put("choices", List.of(choice));
//     response.put("usage", usage);

//     return ResponseEntity.ok(response);
// }

// private String callGemini(String prompt) {
//     try {
//         Map<String, Object> body = Map.of(
//             "contents", List.of(Map.of(
//                 "parts", List.of(Map.of("text", prompt))
//             ))
//         );

//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_JSON);

//         ResponseEntity<Map> resp = new RestTemplate().postForEntity(
//             "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey,
//             new HttpEntity<>(body, headers),
//             Map.class
//         );

//         // List<?> candidates = (List<?>) resp.getBody().get("candidates");
//     Map bodyResp = resp.getBody();
//     if (bodyResp == null || bodyResp.get("candidates") == null) {
//         return "No response from Gemini";
//     }
//     List<?> candidates = (List<?>) bodyResp.get("candidates");
//     if (candidates.isEmpty()) {
//         return "Empty response from Gemini";
//     }
//         Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
//         List<?> parts = (List<?>) content.get("parts");
//         // log.info("Final response: {}", response);
//         return (String) ((Map<?, ?>) parts.get(0)).get("text");

//     } catch (Exception e) {
//         log.error("Gemini failed: {}", e.getMessage());
//         return "Error: " + e.getMessage();
//     }
// }
//     // @PostMapping("/chat/completions")
//     // @Transactional(readOnly = true)
//     // public ResponseEntity<Map<String, Object>> chat(
//     //         @RequestBody Map<String, Object> request) 
//     // {

//     //     // 1. Get user message
//     //     List<Map<String, String>> messages =
//     //         (List<Map<String, String>>) request.get("messages");
//     //     String userMessage = messages.stream()
//     //         .filter(m -> "user".equals(m.get("role")))
//     //         .map(m -> m.get("content"))
//     //         .reduce((a, b) -> b).orElse("");

//     //     log.info("User: {}", userMessage);

//     //     // 2. Get relevant contacts
//     //     List<Contact> contacts =
//     //         contactSearchService.findRelevantContacts(userMessage);
//     //     log.info("Contacts found: {}", contacts.size());

//     //     // 3. Build context
//     //     String context = contacts.stream()
//     //         .map(Contact::toContextString)
//     //         .collect(Collectors.joining("\n---\n"));

//     //     // 4. Build enriched messages with contact context injected
//     //     List<Map<String, String>> enriched = new ArrayList<>();

//     //     // Inject contacts as system message
//     //     enriched.add(Map.of(
//     //         "role", "system",
//     //         "content", context.isBlank()
//     //             ? "You are a contacts assistant. No contacts found for this query."
//     //             : "You are a contacts assistant.\nAnswer using ONLY this data:\n\n" + context
//     //     ));

//     //     // Add original user message
//     //     enriched.add(Map.of("role", "user", "content", userMessage));

//     //     // 5. Forward to LibreChat's Google endpoint
//     //     String answer = forwardToGoogle(enriched);

//     //     return ResponseEntity.ok(Map.of(
//     //         "id", "chatcmpl-" + UUID.randomUUID(),
//     //         "object", "chat.completion",
//     //         "created", System.currentTimeMillis() / 1000,
//     //         "model", "contacts-gemini",
//     //         "choices", List.of(Map.of(
//     //             "index", 0,
//     //             "message", Map.of(
//     //                 "role", "assistant",
//     //                 "content", answer
//     //             ),
//     //             "finish_reason", "stop"
//     //         )),
//     //         "usage", Map.of(
//     //             "prompt_tokens", 0,
//     //             "completion_tokens", 0,
//     //             "total_tokens", 0
//     //         )
//     //     ));
//     // }

//     private String forwardToGoogle(List<Map<String, String>> messages) {
//         // Try LibreChat's internal Google endpoint formats
//         String[] urls = {
//             "http://localhost:3080/api/ask/google",
//             "http://localhost:3080/api/messages",
//             "http://localhost:3080/google/chat/completions"
//         };

//         RestTemplate restTemplate = new RestTemplate();
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_JSON);
//         headers.setBearerAuth(apiKey);

//         // Get last user message as text
//         String userText = messages.stream()
//             .filter(m -> "user".equals(m.get("role")))
//             .map(m -> m.get("content"))
//             .reduce((a, b) -> b).orElse("");

//         String systemText = messages.stream()
//             .filter(m -> "system".equals(m.get("role")))
//             .map(m -> m.get("content"))
//             .findFirst().orElse("");

//         // Format as simple text prompt
//         String fullPrompt = systemText + "\n\nUser: " + userText;

//         Map<String, Object> body = Map.of(
//             "text", fullPrompt,
//             "model", "gemini-2.0-flash",
//             "endpoint", "google"
//         );

//         for (String url : urls) {
//             try {
//                 ResponseEntity<Map> res = restTemplate.postForEntity(
//                     url, new HttpEntity<>(body, headers), Map.class);
//                 if (res.getStatusCode().is2xxSuccessful()) {
//                     return res.getBody().toString();
//                 }
//             } catch (Exception e) {
//                 log.warn("URL {} failed: {}", url, e.getMessage());
//             }
//         }

//         // If all fail, return the contact data directly as plain text
//         return "Based on contacts data:\n" + systemText;
//     }
// }
package com.serri.api.controller;

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

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

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

    @PostMapping("/chat/completions")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request) {

        log.info("=== Chat request received ===");
        log.info("Raw request keys: {}", request.keySet());

        // 1. Safely extract user message
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

        // 2. Find relevant contacts
        List<Contact> contacts = new ArrayList<>();
        try {
            contacts = contactSearchService.findRelevantContacts(userMessage);
        } catch (Exception e) {
            log.error("Error finding contacts: {}", e.getMessage());
        }
        log.info("Contacts found: {}", contacts.size());

        // 3. Build context
        String context = contacts.stream()
            .map(Contact::toContextString)
            .collect(Collectors.joining("\n---\n"));

        // 4. Build prompt
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

    // Ensure the message object is clean
    Map<String, Object> messageMap = new LinkedHashMap<>();
    messageMap.put("role", "assistant");
    messageMap.put("content", answer);

    Map<String, Object> choice = new LinkedHashMap<>();
    choice.put("message", messageMap);
    choice.put("role", "assistant"); 
    choice.put("index", 0);
    choice.put("finish_reason", "stop");

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", "chatcmpl-" + UUID.randomUUID());
    response.put("object", "chat.completion");
    response.put("created", System.currentTimeMillis() / 1000);
    response.put("model", "contacts-gemini"); // Must match your YAML exactly
    response.put("choices", Collections.singletonList(choice)); // Use singletonList for safety
    
    // Usage is mandatory for some UI versions
    Map<String, Object> usage = new HashMap<>();
    usage.put("prompt_tokens", 10);
    usage.put("completion_tokens", 10);
    usage.put("total_tokens", 20);
    response.put("usage", usage);

    return response;
}
    // private Map<String, Object> buildResponse(String answer) {
    //     if (answer == null || answer.isBlank()) {
    //         answer = "I could not process your request. Please try again.";
    //     }

    //     Map<String, Object> messageMap = new LinkedHashMap<>();
    //     messageMap.put("role", "assistant");
    //     messageMap.put("content", answer);

    //     Map<String, Object> choice = new LinkedHashMap<>();
    //     choice.put("index", 0);
    //     choice.put("message", messageMap);
    //     choice.put("finish_reason", "stop");

    //     Map<String, Object> usage = new LinkedHashMap<>();
    //     usage.put("prompt_tokens", 0);
    //     usage.put("completion_tokens", 0);
    //     usage.put("total_tokens", 0);

    //     Map<String, Object> response = new LinkedHashMap<>();
    //     response.put("id", "chatcmpl-" + UUID.randomUUID());
    //     response.put("object", "chat.completion");
    //     response.put("created", System.currentTimeMillis() / 1000);
    //     response.put("model", "contacts-gemini");
    //     response.put("choices", List.of(choice));
    //     response.put("usage", usage);

    //     return response;
    // }

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
}


