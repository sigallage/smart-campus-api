# Smart Campus REST API (JAX-RS / Jersey)

## Overview (API design)

This project is a small RESTful API for managing **rooms**, **sensors**, and **sensor readings** in a smart-campus scenario.

- **Tech stack**: Java 17, JAX-RS (Jersey 3.x), embedded Grizzly HTTP server, JSON via Jackson.
- **Base URL**: `http://localhost:8080/api/v1`
- **Discovery / entry point (hypermedia)**: `GET /api/v1` returns a JSON document with top-level collection links.
- **In-memory persistence**: all data lives in a shared `InMemoryStore` singleton for the lifetime of the process.
- **Error handling**: domain exceptions are mapped to structured JSON (`ApiError`) with appropriate HTTP status codes (e.g., `409`, `422`, `500`).

### Resource model

- **Rooms** (`/rooms`)
	- `GET /rooms` — list rooms
	- `POST /rooms` — create room
	- `GET /rooms/{roomId}` — fetch a room
	- `DELETE /rooms/{roomId}` — delete a room (blocked with `409` if it has ACTIVE sensors)
- **Sensors** (`/sensors`)
	- `GET /sensors` — list sensors
	- `GET /sensors?type=CO2` — filter sensors by type
	- `POST /sensors` — create sensor (requires an existing `roomId`, otherwise `422`)
- **Sensor readings** (sub-resource locator)
	- `GET /sensors/{sensorId}/readings` — list readings for a sensor
	- `POST /sensors/{sensorId}/readings` — append a reading (auto-generates `id` and `timestamp` if omitted)

## Build and run (step-by-step)

### Prerequisites

1. Install **JDK 17**.
2. Install **Apache Maven**.
3. From the repo root, verify versions:

```bash
java -version
mvn -v
```

### Build

1. From the repo root, run:

```bash
mvn clean test
```

2. Create a runnable shaded JAR:

```bash
mvn clean package
```

This produces `target/smart-campus-api-1.0.0-SNAPSHOT-all.jar`.

### Run the server

Option A (recommended during development):

```bash
mvn exec:java
```

Option B (run the packaged JAR):

```bash
java -jar target/smart-campus-api-1.0.0-SNAPSHOT-all.jar
```

The server listens on port **8080** and prints:

```
Smart Campus API started: http://0.0.0.0:8080/api/v1
```

## Sample curl commands (successful)

All examples assume the server is running locally and use `http://localhost:8080/api/v1`.
If you are using **PowerShell**, you may need to run `curl.exe` (because `curl` can be an alias for `Invoke-WebRequest`).

1) Discovery entry point:

```bash
curl http://localhost:8080/api/v1
```

2) Create a room:

```bash
curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d '{"id":"R1","name":"Lab 1","capacity":30,"regulations":"No food"}'
```

3) List rooms:

```bash
curl http://localhost:8080/api/v1/rooms
```

4) Create a sensor (linked to `R1`):

```bash
curl -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d '{"id":"S1","type":"CO2","roomId":"R1"}'
```

5) List sensors (and filter by type):

```bash
curl http://localhost:8080/api/v1/sensors
curl "http://localhost:8080/api/v1/sensors?type=CO2"
```

6) Append a sensor reading (id + timestamp are optional):

```bash
curl -X POST http://localhost:8080/api/v1/sensors/S1/readings -H "Content-Type: application/json" -d '{"value":412.5}'
```

7) List readings for a sensor:

```bash
curl http://localhost:8080/api/v1/sensors/S1/readings
```

8) Create and then delete an empty room (demonstrates `DELETE`):

```bash
curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d '{"id":"R2","name":"Meeting Room","capacity":8}'

curl -i -X DELETE http://localhost:8080/api/v1/rooms/R2
```

---

## Coursework report Q&A

Question: In your report, explain the default lifecycle of a JAX-RS Resource class. Is a
new instance instantiated for every incoming request, or does the runtime treat it as a
singleton? Elaborate on how this architectural decision impacts the way you manage and
synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.
In a typical JAX‑RS runtime (including Jersey), resource classes are request-scoped by default: the framework may construct a new resource instance per incoming HTTP request and then discard it after the response is produced. In this model, the resource object itself should be treated as short‑lived and not relied on to hold shared state across requests. If a developer wants singleton-like behavior, that must be explicitly configured (for example by registering a single instance or using a dependency-injection scope that is singleton), and then the same resource instance may handle many requests concurrently.
In this codebase, even though resource instances are not designed to be singletons, the API still has shared mutable state because all requests interact with the same InMemoryStore singleton (InMemoryStore.getInstance()). Since the server can process multiple requests at the same time (different threads), the shared store must be safe for concurrent access. The implementation uses concurrency-friendly collections (e.g., ConcurrentHashMap for entity storage and CopyOnWriteArrayList for sensor readings), which helps prevent basic data corruption when multiple requests read/write the store concurrently.
However, request-scoped resources do not automatically prevent race conditions in multi-step business rules. Any “check-then-act” sequence can still be interleaved with another request. For example, a room deletion that first checks whether a room has active sensors and then deletes the room can be invalidated if another request creates/activates a sensor in that room between those two steps. In addition, objects stored inside concurrent maps (like Room and Sensor) remain mutable, so updates such as setting a sensor’s current value can race (“last write wins”), and nested mutable structures (like a room’s list of sensor IDs) need careful coordination to avoid inconsistent reads during serialization. The architectural takeaway is that correctness depends on making shared operations atomic (via appropriate locking or atomic update patterns) and keeping shared data structures consistently thread-safe, not on the lifecycle of the resource class itself.

Question: Why is the provision of ”Hypermedia” (links and navigation within responses)
considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach
benefit client developers compared to static documentation?
The API exposes a root “Discovery” endpoint at GET /api/v1 that returns a JSON document containing essential metadata (API name, version, timestamp, and administrative contact) and, crucially, a map of primary resource collections such as rooms and sensors expressed as fully-qualified URLs. Because these links are built dynamically from the runtime base URI, the discovery response remains correct across different deployment environments (e.g., localhost vs. a hosted domain), and clients do not need to hardcode the server host or base path to locate core collections.
This approach is aligned with hypermedia-driven REST ideas (often discussed under HATEOAS) because the server provides navigational links that clients can follow to discover available actions/resources. In other words, the API is not only sending data; it is also sending “where to go next,” enabling a client to start from a single entry point and learn the addresses of key collections at runtime. In this project, the discovery document is a clear “entry point” that supports link-based navigation, even if other representations (e.g., individual room/sensor payloads) do not yet embed richer link relations like self, readings, or related resources.
Compared to static documentation, hypermedia-style discovery reduces coupling and improves resilience to change. If collection paths or deployment base URLs evolve, clients that rely on discovery links can continue to function with minimal or no updates, because the server remains the source of truth for current routes. It also improves developer experience by making the API more self-describing: client developers can programmatically locate resources and traverse the API without relying solely on external docs that may drift out of date.
Question: When returning a list of rooms, what are the implications of returning only
IDs versus returning the full room objects? Consider network bandwidth and client side
processing.
In this implementation, GET /api/v1/rooms returns a full list of Room objects (the resource simply returns store.listRooms(), and the store returns the current Room values). Returning full objects is convenient for clients because it minimizes follow-up requests: the client can render room name, capacity, and any other metadata immediately without having to call GET /api/v1/rooms/{roomId} for each room. This reduces client-side orchestration complexity and improves perceived performance in small to medium datasets.
However, returning full objects has bandwidth and processing tradeoffs. Payloads grow as the number of rooms increases and as each room representation becomes richer (for example, if rooms embed sensor IDs). Larger responses increase network transfer time and client parsing cost, and they can waste bandwidth when the client only needs a subset of fields (e.g., just id and name for a dropdown). By contrast, returning only IDs (or a “summary” projection) keeps responses smaller and faster to transfer, but it shifts work to the client: the client must either make many additional requests to fetch details (creating a “chatty” N+1 pattern) or implement batching strategies. In practice, a common compromise is to return summary objects in the collection (id + name + key fields) and allow clients to retrieve full details via GET /rooms/{id} when needed, which balances bandwidth efficiency against client complexity.

Question: Is the DELETE operation idempotent in your implementation? Provide a detailed
justification by describing what happens if a client mistakenly sends the exact same DELETE
request for a room multiple times.
DELETE /api/v1/rooms/{roomId} implementation behaves as an idempotent operation in the HTTP sense: once the room has been deleted, repeating the exact same DELETE request does not cause additional state changes on the server (the room is already absent). In other words, after the first successful deletion, subsequent identical DELETEs leave the system in the same “room does not exist” state, which is the core property of idempotency.
Concretely, the first DELETE request follows this logic: if the room exists and it does not have any ACTIVE sensors assigned, the store removes it and the service returns 204 No Content. If the room has ACTIVE sensors, the deletion is blocked by throwing RoomNotEmptyException, which is mapped to a 409 Conflict JSON error response (via RoomNotEmptyExceptionMapper), preventing orphaned sensor relationships. If the client mistakenly sends the same DELETE multiple times: after a successful deletion, the next DELETE finds the room missing and returns 404 Not Found (with your standard error body), while the server state remains unchanged. It’s worth noting that idempotent methods are allowed to return different status codes across repeated calls; what must remain consistent is the final server state (deleted/not deleted), which your implementation preserves.

Question: We explicitly use the @Consumes (MediaType.APPLICATION_JSON) annotation on
the POST method. Explain the technical consequences if a client attempts to send data in
a different format, such as text/plain or application/xml. How does JAX-RS handle this
mismatch?
In SensorResource implementation, the sensor-creation endpoint is explicitly constrained to accept JSON by annotating the POST method with @Consumes(MediaType.APPLICATION_JSON) (see SensorResource.java). This is an important integrity and interoperability choice: it makes the contract unambiguous that the request entity must be JSON, and it ensures Jersey selects the correct message-body reader (Jackson, which you registered) to deserialize the payload into a Sensor object.
If a client sends the same logical data but labels it with a different Content-Type such as text/plain or application/xml, Jersey will treat that as a media type mismatch for this method. Because the method declares it only consumes JSON, the runtime will typically reject the request with 415 Unsupported Media Type (the server is saying “I do not accept this request format for this endpoint”). This behavior prevents accidental misinterpretation of payloads and avoids “best guess” parsing, which could otherwise lead to incorrect object construction or hidden errors. If the client does send Content-Type: application/json but the body is malformed JSON (or does not match the expected Sensor structure), the request can instead fail during deserialization and be treated as a client error (commonly 400 Bad Request). In short: @Consumes protects the endpoint by enforcing that only JSON payloads are considered valid input, and other formats are rejected early and predictably.

Question: You implemented this filtering using @QueryParam. Contrast this with an alternative
design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why
is the query parameter approach generally considered superior for filtering and searching
collections?
GET /api/v1/sensors endpoint supports optional filtering using @QueryParam("type") (see SensorResource.java). This design fits REST conventions because the path (/sensors) identifies the collection resource, while the query string expresses an optional “search/filter view” over that same collection. In your code, if type is absent or blank you return all sensors; if it is present, you normalize it and return only matching sensors—this is exactly the behavior expected of an optional filter.
Compared with a path-based design like /api/v1/sensors/type/CO2, query parameters are generally preferred for filtering and searching because they scale better and keep URIs stable. Filters tend to multiply over time (e.g., type, status, roomId, paging, sorting), and encoding them in the path quickly leads to awkward endpoint proliferation and a combinatorial explosion of route variants. Query parameters handle multiple optional criteria naturally (e.g., /sensors?type=CO2&status=ACTIVE) without introducing new “pseudo-hierarchy” levels that are not real resources. This also avoids ambiguity with legitimate identifiers (a path segment can conflict with a future sensorId pattern) and keeps the API easier to evolve: adding a new filter does not require introducing a new route structure, only a new optional query parameter with predictable semantics.

Question: Discuss the architectural benefits of the Sub-Resource Locator pattern. How
does delegating logic to separate classes help manage complexity in large APIs compared
to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller
class?
In the API, SensorResource exposes a sub-resource locator at @Path("{sensorId}/readings") which returns a dedicated SensorReadingResource instance (see SensorResource.java). This pattern has a clear architectural benefit: it lets the top-level resource (/sensors) focus on sensor collection and sensor creation concerns, while delegating the nested “readings under a specific sensor” behavior to a separate class that is scoped to that sensorId context.
This separation reduces complexity as the API grows. Without sub-resources, a single controller/resource class often becomes a large “god class” containing every nested route (/sensors/{id}, /sensors/{id}/readings, /sensors/{id}/readings/{rid}, etc.), mixing multiple responsibilities and increasing the risk of duplicated validation and inconsistent behavior. With your approach, the SensorReadingResource constructor receives the sensorId plus the shared store, meaning all reading-related endpoints naturally share the same contextual setup and can consistently apply sensor-level checks (e.g., “sensor exists”, “sensor in MAINTENANCE”) in one place. This improves maintainability, makes the codebase easier to test and extend, and keeps route ownership clear: “sensor collection logic” stays in SensorResource, and “readings logic” lives in SensorReadingResource.

Question: Why is HTTP 422 often considered more semantically accurate than a standard
404 when the issue is a missing reference inside a valid JSON payload?
In the API, creating a sensor is a POST /api/v1/sensors where the request body contains a roomId. When the JSON payload is well-formed and syntactically valid, but the referenced room does not exist, the problem is not “the endpoint does not exist” — it is that the server cannot process the entity as submitted because it violates a domain constraint (“sensor must link to a real room”). That is exactly what 422 Unprocessable Entity is meant to express: the representation is readable and valid JSON, but it fails semantic validation rules.
The code implements this distinction explicitly: SensorResource throws a LinkedResourceNotFoundException when roomId doesn’t exist, and LinkedResourceNotFoundExceptionMapper.java maps it to 422 with a structured JSON error body (ApiError). By contrast, a 404 Not Found is typically used when the requested URI has no matching resource (e.g., GET /rooms/{id} for an ID that isn’t present). Using 422 here communicates that /sensors is a valid route and the JSON was accepted as JSON, but the relationship inside the payload is invalid, which is clearer for client developers than treating it as a generic “not found” response.

Question: From a cybersecurity standpoint, explain the risks associated with exposing
internal Java stack traces to external API consumers. What specific information could an
attacker gather from such a trace?
Exposing stack traces to external clients is risky because stack traces often leak internal implementation details that attackers can use to plan targeted exploits. For example, a stack trace can reveal internal package/class names, framework versions, exact dependency choices, server configuration, file paths, and sometimes even sensitive fragments like SQL query structures, connection details, or internal hostnames. This information reduces attacker effort by turning “black box” guessing into “white box” reconnaissance: it helps them identify known-vulnerable libraries, locate weak input-validation points, and craft payloads that trigger specific failure paths.
The code mitigates this by design. The global safety-net mapper in GlobalThrowableMapper.java logs the full exception server-side (so developers still have debugging detail) but returns a generic 500 JSON response ("An unexpected error occurred.") to the client. This achieves the coursework goal of being “leak-proof”: clients get a consistent error contract, while sensitive diagnostic details remain internal.

Question: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like
logging, rather than manually inserting Logger.info() statements inside every single resource
method?
Request/response logging is a classic cross-cutting concern: it applies uniformly to every endpoint regardless of business logic. Implementing logging via a JAX-RS filter centralizes that concern in one place, ensuring consistent coverage and formatting across the entire API and avoiding duplicated code in every resource method. It also improves maintainability: adding new endpoints automatically inherits logging behavior without requiring developers to remember to insert logging statements each time.
The implementation in ApiLoggingFilter.java shows the practical advantage: it logs the incoming HTTP method + URL for every request, and then logs the final response status code for every response. Doing this manually inside each endpoint is error-prone (some methods will forget, logs may be inconsistent, and exceptions can bypass “end of method” logs). With filters, you get reliable observability across normal flows and error flows, while keeping resource classes focused on domain behavior rather than infrastructure concerns.
