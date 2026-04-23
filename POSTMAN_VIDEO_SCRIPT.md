# Smart Campus API — Postman Video Demonstration Script

This is a **recording script** (what to click + what to say) that demonstrates each coursework task using Postman.

## 0) Pre-flight (before recording)

### Start the API
1. Open a terminal in the project root.
2. Run:
   - `mvn -q package`
   - `java -jar target/smart-campus-api-1.0.0-SNAPSHOT-all.jar`
3. Confirm the console prints:
   - `Smart Campus API started: http://0.0.0.0:8080/api/v1`

### Postman setup (do once)
1. Create a Postman **Environment** named `SmartCampus Local`.
2. Add variables:
   - `baseUrl` = `http://localhost:8080/api/v1`
   - `roomId` = (leave blank)
   - `sensorIdActive` = (leave blank)
   - `sensorIdMaint` = (leave blank)
3. Create a **Collection** named `SmartCampus API`.

Tip: For every JSON request, set header `Content-Type: application/json`.

---

## Part 1 — Service Architecture & Setup

### 1A) Discovery endpoint (GET /api/v1)
**Request**
- Method: `GET`
- URL: `{{baseUrl}}`

**What to say (narration)**
- “This API is versioned under `/api/v1` via the JAX-RS `@ApplicationPath("/api/v1")` configuration.”
- “This is the Discovery endpoint at `GET /api/v1`. It returns API metadata plus a map of primary resource links.”
- “This is an example of hypermedia: clients can discover important URLs from the response instead of hard-coding them.”

**What to show (expected output)**
- JSON includes `name`, `version`, `contact`, and `resources.rooms` + `resources.sensors`.

### 1B) Lifecycle question (say this in your video or report)
- “By default, Jersey/JAX-RS resource classes are typically **request-scoped** (a new instance per request), unless configured as singletons (e.g., `@Singleton` or registering a singleton instance).”
- “Even with request-scoped resources, any shared state (like my `InMemoryStore` singleton) can be accessed concurrently by many threads, so the **store must be thread-safe**.”
- “That’s why the in-memory maps/lists use concurrent collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) and minimal synchronization where needed.”

---

## Part 2 — Room Management

### 2A) List rooms (GET /rooms)
**Request**
- Method: `GET`
- URL: `{{baseUrl}}/rooms`

**Narration**
- “This returns the full list of rooms as JSON.”

### 2B) Create a room (POST /rooms)
**Request**
- Method: `POST`
- URL: `{{baseUrl}}/rooms`
- Body (raw JSON):
```json
{
  "id": "R101",
  "name": "Engineering Lab",
  "capacity": 30,
  "regulations": "No food or drink"
}
```

**Postman action**
- After sending, set `roomId` to `R101` in the environment.

**Narration**
- “A successful create returns `201 Created`, includes a `Location` header, and returns the created room object.”

### 2C) Get a specific room (GET /rooms/{roomId})
**Request**
- Method: `GET`
- URL: `{{baseUrl}}/rooms/{{roomId}}`

**Narration**
- “This returns detailed metadata for a single room.”

### 2D) Delete a room (DELETE /rooms/{roomId})
For the rest of this demo we still need room `R101`, so we’ll demonstrate DELETE using a temporary room.

**Step 1: Create a temporary room**
- Method: `POST`
- URL: `{{baseUrl}}/rooms`
```json
{
  "id": "R-DEL-1",
  "name": "Temporary Room",
  "capacity": 1,
  "regulations": "Demo only"
}
```

**Step 2: Delete it (first time)**
- Method: `DELETE`
- URL: `{{baseUrl}}/rooms/R-DEL-1`

**Step 3: Delete it again (same request repeated)**
- Method: `DELETE`
- URL: `{{baseUrl}}/rooms/R-DEL-1`

**Narration (idempotency question)**
- “DELETE is defined as **idempotent** in HTTP: repeating the same delete should not keep changing server state after the first deletion.”
- “Here, the first delete returns `204 No Content`. Repeating it returns `404 Not Found` because the room is already deleted — but the server state is unchanged, so it’s still idempotent in terms of system state.”

### 2E) Rooms list: IDs vs full objects (say this)
- “Returning only IDs reduces bandwidth and speeds up list retrieval, but clients then need extra requests to fetch details.”
- “Returning full objects is heavier but reduces follow-up calls. A common compromise is a lightweight summary projection in the list response.”

---

## Part 3 — Sensor Operations & Linking

### 3A) Create a sensor (POST /sensors)
**Request**
- Method: `POST`
- URL: `{{baseUrl}}/sensors`
- Body (raw JSON):
```json
{
  "id": "S-CO2-1",
  "type": "CO2",
  "roomId": "R101",
  "status": "ACTIVE"
}
```

**Postman action**
- Set `sensorIdActive` = `S-CO2-1`.

**Narration**
- “When I POST a sensor, the API validates that the `roomId` exists (link integrity). On success I get `201 Created`.”

### 3B) List sensors (GET /sensors)
**Request**
- Method: `GET`
- URL: `{{baseUrl}}/sensors`

### 3C) Filter sensors by type (GET /sensors?type=CO2)
**Request**
- Method: `GET`
- URL: `{{baseUrl}}/sensors?type=CO2`

**Narration (query-param vs path question)**
- “Filtering is typically done with query parameters because it keeps the resource path stable (`/sensors`) while expressing a *search constraint* (`type=CO2`).”
- “Path segments are better for identifying a specific resource, not for optional filters that can be combined.”

### 3D) Demonstrate 422 linked resource validation
**Request**
- Method: `POST`
- URL: `{{baseUrl}}/sensors`
- Body:
```json
{
  "id": "S-BAD-1",
  "type": "CO2",
  "roomId": "ROOM_DOES_NOT_EXIST"
}
```

**Narration**
- “This returns `422 Unprocessable Entity` because the JSON payload is syntactically valid, but it references a linked resource (`roomId`) that doesn’t exist.”

### 3E) @Consumes mismatch explanation (say this)
- “`@Consumes(MediaType.APPLICATION_JSON)` tells JAX-RS the method only accepts JSON.”
- “Normally, if a client sends `text/plain` or `application/xml`, the runtime rejects the request (commonly `415 Unsupported Media Type`).”
- “In *this* project, there is also a global catch-all exception mapper that converts unexpected exceptions into a safe JSON response. So you may see a safe `500` JSON error instead of a raw framework error page.”

---

## Part 4 — Deep Nesting with Sub-Resources (Readings)

### 4A) List readings for a sensor (GET /sensors/{sensorId}/readings)
**Request**
- Method: `GET`
- URL: `{{baseUrl}}/sensors/{{sensorIdActive}}/readings`

**Narration (sub-resource locator benefits)**
- “`/sensors/{id}/readings` is implemented using the sub-resource locator pattern.”
- “This keeps the sensor collection logic separate from reading-history logic, avoiding one massive controller class and keeping nested paths maintainable.”

### 4B) Append a reading (POST /sensors/{sensorId}/readings)
**Request**
- Method: `POST`
- URL: `{{baseUrl}}/sensors/{{sensorIdActive}}/readings`
- Body:
```json
{
  "value": 650.5
}
```

**Narration**
- “A successful POST appends the reading and returns `201 Created`.”
- “Side-effect: it updates the parent sensor’s `currentValue` to match the latest reading.”

### 4C) Prove the side-effect (GET /sensors)
**Request**
- Method: `GET`
- URL: `{{baseUrl}}/sensors`

**What to show**
- The sensor `S-CO2-1` now has `currentValue` set to the reading value.

---

## Part 5 — Advanced Error Handling, Exception Mapping & Logging

### 5A) 409 Conflict — Room cannot be deleted while occupied

**Step 1: Create a new room**
- `POST {{baseUrl}}/rooms`
```json
{
  "id": "R200",
  "name": "Server Room",
  "capacity": 5,
  "regulations": "Authorized staff only"
}
```

**Step 2: Create an ACTIVE sensor in that room**
- `POST {{baseUrl}}/sensors`
```json
{
  "id": "S-ACTIVE-200",
  "type": "TEMP",
  "roomId": "R200",
  "status": "ACTIVE"
}
```

**Step 3: Attempt deletion (should fail)**
- Method: `DELETE`
- URL: `{{baseUrl}}/rooms/R200`

**Narration**
- “This triggers a custom exception mapped to `409 Conflict` with a structured JSON error body.”

### 5B) 403 Forbidden — Sensor in MAINTENANCE rejects readings

**Step 1: Create a MAINTENANCE sensor**
- `POST {{baseUrl}}/sensors`
```json
{
  "id": "S-MAINT-1",
  "type": "CO2",
  "roomId": "R101",
  "status": "MAINTENANCE"
}
```

**Step 2: Try to post a reading**
- `POST {{baseUrl}}/sensors/S-MAINT-1/readings`
```json
{
  "value": 500
}
```

**Narration**
- “Because the sensor is in MAINTENANCE, the API returns `403 Forbidden` using a dedicated exception mapper.”

### 5C) 500 Internal Server Error — Global safety net

**Postman request to demonstrate `500`**
- Method: `POST`
- URL: `{{baseUrl}}/sensors`
- Header: `Content-Type: text/plain`
- Body (raw):
```
hello
```

**Narration**
- “This request is intentionally invalid for the JSON-only endpoint. The server’s global exception mapper catches the unexpected failure and returns a leak-proof JSON `500` response instead of exposing a stack trace.”

### 5D) Request/Response logging filter

**What to do**
- Keep the server console visible.
- Send any request (e.g., `GET {{baseUrl}}`).

**What to show**
- Console logs similar to:
  - `REQUEST GET http://localhost:8080/api/v1`
  - `RESPONSE 200 GET http://localhost:8080/api/v1`

**Narration (filters vs manual logging question)**
- “Filters are ideal for cross-cutting concerns: one place to log every request/response consistently.”
- “This avoids repeating `logger.info(...)` in every resource method and reduces the chance of missing endpoints or producing inconsistent logs.”

---

## Quick cleanup (optional)
- Stop the server with `Ctrl+C`.

## If something goes wrong
- If Postman says “Could not get any response”, verify the server is running and `baseUrl` is correct.
- If you get `404`, confirm you’re calling `/api/v1/...` (not `/api/...` or `/v1/...`).
