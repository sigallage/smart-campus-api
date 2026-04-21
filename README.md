# Smart Campus — Sensor & Room Management API (JAX-RS)

## Run

- Build a runnable fat JAR:
  - `mvn -q package`
- Start the server:
  - `java -jar target/smart-campus-api-1.0.0-SNAPSHOT-all.jar`
- API base path:
  - `http://localhost:8080/api/v1`

## Implemented (Coursework Part 1)

- JAX-RS application configured with `@ApplicationPath("/api/v1")`
- Root “Discovery” endpoint: `GET /api/v1` returns API metadata and primary resource links
