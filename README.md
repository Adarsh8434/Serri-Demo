# LibreChat Contacts Integration

A Spring Boot backend that integrates a Contacts workspace with LibreChat,
enabling the AI assistant to answer questions about stored contacts during
normal chat interactions.

---

## Architecture
┌─────────────────────────────────────────────────────────┐
│                    LibreChat (port 3080)                  │
│         Chat UI powered by Google Gemini                  │
│         "Contacts Assistant" custom endpoint              │
└──────────────────────┬──────────────────────────────────┘
│ POST /librechat/chat/completions
▼
┌─────────────────────────────────────────────────────────┐
│               Spring Boot Backend (port 8080)             │
│                                                           │
│  1. Extract user message from request                     │
│  2. Search PostgreSQL contacts DB for relevant matches    │
│  3. Inject matching contacts into Gemini prompt           │
│  4. Call Google Gemini API                                │
│  5. Return OpenAI-compatible response to LibreChat        │
└──────────────────────┬──────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL Database                          │
│  - contacts table (core fields)                           │
│  - contact_attributes table (arbitrary key-value)         │
└─────────────────────────────────────────────────────────┘

### Chat Integration Design Decision

**Approach: Retrieval + Prompt Context Injection**

When a user sends a message in LibreChat using the "Contacts Assistant":

1. Spring Boot receives the message from LibreChat
2. Keywords are extracted from the user query (stop words removed)
3. PostgreSQL is searched across name, company, city, state, role, attributes
4. Top 20 matching contacts are retrieved
5. Contact data is injected into the Gemini system prompt
6. Only relevant contacts are sent — not all contacts (extra credit)
7. Gemini answers based solely on the injected contact context

**Why this approach:**
- Simple and reliable — no complex tool/function calling setup needed
- Token efficient — only relevant contacts sent, not all 1M
- Accurate — LLM grounded in real data, prevents hallucination
- Scalable — keyword extraction + DB search handles large datasets

---

## Project Structure
LibreChat-Contacts-Demo/
├── src/main/java/com/serri/api/
│   ├── controller/
│   │   ├── ContactController.java        # CRUD + search + context
│   │   ├── CsvImportController.java      # CSV bulk import
│   │   └── LibreChatProxyController.java # LibreChat integration
│   ├── service/
│   │   ├── ContactService.java           # DTO → Entity
│   │   ├── CSVImportService.java         # CSV parsing
│   │   ├── BatchSaveService.java         # Batch DB saves
│   │   └── ContactSearchService.java     # Smart keyword search
│   ├── model/
│   │   ├── Contact.java                  # Core contact entity
│   │   └── ContactAttribute.java         # Arbitrary attributes
│   ├── repository/
│   │   ├── ContactRepository.java        # JPA + search query
│   │   └── ContactAttributeRepository.java
│   └── dto/
│       ├── ContactDTO.java
│       ├── ContactResponseDTO.java
│       ├── ImportResult.java
│       └── PageContactResponse.java
├── src/main/resources/
│   ├── static/
│   │   ├── index.html                    # Contacts UI
│   │   └── .well-known/openapi.yaml      # OpenAPI spec
│   └── application.properties
└── README.md

---

## Setup Instructions

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Docker Desktop (for LibreChat)
- Git

### 1. Database Setup
```sql
psql -U postgres
CREATE DATABASE contacts_db;
\q
```

### 2. Configure `application.properties`
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/contacts_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword

# Google Gemini API key (from https://aistudio.google.com/app/apikey)
gemini.api.key=YOUR_GEMINI_KEY_HERE
```

### 3. Run Spring Boot
```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

Spring Boot runs on **http://localhost:8080**

### 4. Setup LibreChat (Docker)
```bash
cd LibreChat

# Create override file
echo "services:
  api:
    volumes:
      - type: bind
        source: ./librechat.yaml
        target: /app/librechat.yaml" > docker-compose.override.yml

# Configure librechat.yaml with Contacts Assistant endpoint
# (see librechat.yaml in this repo)

docker compose up -d
```

LibreChat runs on **http://localhost:3080**

### 5. Import Contacts
```bash
# Import 1k contacts
curl -X POST http://localhost:8080/api/contacts/import \
  -F "file=@chat_states_1k.csv"

# Import 10k contacts
curl -X POST http://localhost:8080/api/contacts/import \
  -F "file=@chat_states_10k.csv"
```

CSV Download Links:
- https://storage.googleapis.com/assignment-input-files-serri/chat_states_1k.csv
- https://storage.googleapis.com/assignment-input-files-serri/chat_states_10k.csv
- https://storage.googleapis.com/assignment-input-files-serri/chat_states_1M.csv

### 6. Use in LibreChat

Open http://localhost:3080
Click model selector
Select "Contacts Assistant"
Ask questions about contacts:

"Who is from Jaipur?"
"List contacts from Rajasthan"
"Who works at Palla-Kulkarni?"
"Who has LEAD-NEW status?"




---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/contacts` | List contacts (paginated) |
| GET | `/api/contacts/{id}` | Get single contact |
| POST | `/api/contacts` | Create contact |
| PUT | `/api/contacts/{id}` | Update contact |
| DELETE | `/api/contacts/{id}` | Delete contact |
| GET | `/api/contacts/search?q=` | Search contacts |
| GET | `/api/contacts/search-simple?q=` | Simple search for AI |
| POST | `/api/contacts/import` | Import CSV file |
| GET | `/librechat/models` | LibreChat model list |
| POST | `/librechat/chat/completions` | LibreChat chat endpoint |

---

## Design Questions

### 1. If the system needed to support 1,000,000 contacts, how would you redesign it?

- **Vector embeddings**: Generate embeddings for each contact at import time using a model like `text-embedding-3-small`. Store in PostgreSQL with `pgvector` extension. At query time, embed the user query and run cosine similarity search — this finds semantically relevant contacts even with synonyms.
- **Full-text search**: Add PostgreSQL `tsvector` indexes on name, company, city fields for fast keyword search.
- **Async CSV import**: Use Spring Batch with partitioned steps to process 1M rows in parallel chunks. Each chunk runs in its own transaction to avoid memory issues.
- **Pagination and limits**: Never load all contacts into memory. Always query with `LIMIT` and use cursor-based pagination.
- **Caching**: Add Redis cache for frequent queries like "who is from Jaipur" — cache results for 5 minutes.
- **Database partitioning**: Partition the contacts table by `created_at` or by state/region for faster range queries.

### 2. How would you ensure the assistant retrieves the most relevant contacts for a query?

- **Semantic search with embeddings**: Instead of keyword matching, embed the user query and find contacts with highest cosine similarity to the query embedding. This handles synonyms — "software engineer" matches "developer", "programmer".
- **Hybrid search**: Combine keyword search (BM25/full-text) with semantic search (vector similarity) using Reciprocal Rank Fusion to get best of both approaches.
- **Query understanding**: Use an LLM to extract structured filters from the natural language query before searching — e.g., "CTOs in Bangalore" → `{role: CTO, city: Bangalore}`.
- **Re-ranking**: After retrieving top-K candidates, use a cross-encoder model to re-rank by relevance to the original query.
- **Feedback loop**: Track which contacts the user actually asked follow-up questions about and use that signal to improve ranking.

### 3. What are the limitations of your current implementation?

- **Keyword matching only**: Current search uses SQL `LIKE` queries — it misses semantic similarity. "software engineer" won't match "developer".
- **No multi-user isolation**: All users share the same contacts database. A production system needs per-user or per-organization contact scoping.
- **Token limit risk**: Injecting 20 contacts into a prompt is fine for small contacts, but if contacts have long notes/attributes, the prompt could exceed the LLM context window.
- **No contact deduplication**: Re-importing a CSV creates duplicate contacts. Need upsert logic based on email or phone.
- **Synchronous import**: Large CSV files (1M rows) block the HTTP request. Should use async processing with a job queue and progress tracking endpoint.
- **No authentication**: The Spring Boot API has no auth — any user can access or delete all contacts.
- **Gemini key**: The provided `AQ.` key does not work directly with Google's Generative AI API. A standard Google AI Studio key is required for the chat integration to function.

---

## Bonus Features Implemented

- ✅ Contact search (keyword search across all fields)
- ✅ Contact editing and deletion
- ✅ Arbitrary attributes (stored as key-value in separate table)
- ✅ Improved UI for browsing large contact lists (pagination)
- ✅ Smart contact retrieval — only relevant contacts sent to LLM (Extra Credit)
- ✅ CSV import supporting 1k, 10k, 1M contacts with batch processing
