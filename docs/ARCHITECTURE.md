# Architecture

## 1. Big picture (production)

```mermaid
flowchart LR
    User["Client<br/>(browser, Swagger UI, Postman)"]
    subgraph Railway["Railway project"]
        App["Spring Boot app<br/>Docker container<br/>profile: prod"]
        DB[("Railway MySQL 8<br/>managed database")]
    end
    GitHub["GitHub repository"]

    User -- "HTTPS" --> App
    App -- "JDBC over Railway's private network" --> DB
    GitHub -- "push triggers build from Dockerfile" --> App
```

- Railway builds the image from the `Dockerfile` in the repository and runs it.
- The public HTTPS address is created by Railway (it ends HTTPS at its proxy and forwards plain HTTP to the container).
- The app talks to MySQL over the private network; the database is not meant to be used by browsers.

## 2. Inside the application

```mermaid
flowchart TB
    Req["HTTP request"] --> JF["JwtAuthenticationFilter<br/>reads Authorization: Bearer token"]
    JF --> SC["Spring Security rules<br/>public: /auth, Swagger, /actuator/health<br/>everything else needs a valid token"]
    SC --> C["Controllers<br/>@PreAuthorize (USER / ADMIN)<br/>validation, status codes"]
    C --> S["Services<br/>business rules, @Transactional"]
    S --> R["Spring Data JPA repositories"]
    R --> DB[("MySQL")]
    C -. "any exception" .-> EH["GlobalExceptionHandler<br/>same JSON error shape, no stack traces"]
    S -. "any exception" .-> EH
```

| Layer | Package | Job |
|-------|---------|-----|
| Security | `security` | JWT creation and checking, filter, BCrypt, 401 / 403 JSON answers |
| Controller | `controller` | HTTP only. Role rules with `@PreAuthorize`. Swagger annotations |
| Service | `service` | All business rules (ride status changes, driver availability, duplicates) |
| Repository | `repository` | Spring Data JPA, paging, the JOIN FETCH query for the N+1 fix |
| Model | `model` | JPA entities (`Driver`, `Ride`, `VehicleType`, `AppUser`) |
| DTO | `dto` | Request and response classes. Entities are never returned directly |
| Config | `config` | OpenAPI settings, first admin account |
| Exception | `exception` | Custom exceptions and the global handler |

## 3. Login and token flow

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthController / AuthService
    participant D as MySQL
    C->>A: POST /api/v1/auth/login (username, password)
    A->>D: find user, compare BCrypt hash
    D-->>A: user
    A-->>C: 200 { token, expiresInMinutes, role }
    C->>A: GET /api/v1/drivers  + Authorization: Bearer token
    Note over A: filter checks signature and expiry,<br/>@PreAuthorize checks the role
    A->>D: SELECT ... LIMIT
    A-->>C: 200 page of drivers
```

## 4. Database

```mermaid
erDiagram
    DRIVERS ||--o{ RIDES : "has (driver_id, owning side)"
    DRIVERS }o--o{ VEHICLE_TYPES : "driver_vehicle_types"
    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password "BCrypt hash"
        varchar role "ADMIN or USER"
    }
    DRIVERS {
        bigint id PK
        varchar email UK
        varchar license_number UK
        boolean available
    }
    RIDES {
        bigint id PK
        bigint driver_id FK
        varchar status
        decimal fare
        datetime booking_time
    }
    VEHICLE_TYPES {
        bigint id PK
        varchar name UK
    }
```

Flyway owns the schema (`V1` to `V10`). Hibernate only validates it (`ddl-auto=validate`).

## 5. Local versus production

| | Local | Production (Railway) |
|---|---|---|
| Database | MySQL Server on your PC, managed with MySQL Workbench | Railway MySQL |
| Profile | none (default) | `prod` (set in the Dockerfile) |
| Port | 8080 | `PORT` given by Railway |
| SQL logging | on (to study the N+1 problem) | off |
| Config source | IntelliJ environment variables | Railway service variables |
| Same in both | MySQL 8, Flyway migrations, code, JWT rules | |
