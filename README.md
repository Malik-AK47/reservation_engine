# Concurrency-Safe Reservation Engine

A Spring Boot backend for reserving limited resources (such as seats at an event) where **two users can never successfully claim the same resource**, even when requests race, clients retry, or a transaction fails.

> **Project status: in active development.** The database schema and design are complete; the reservation workflows and concurrency tests are being built. See [Project Status](#project-status) for exactly what works today. Nothing below is claimed as finished unless it is marked ✅.

---

## Why this project exists

A naive booking flow does this:

1. Check whether the seat is available.
2. Insert a booking.

Two requests can both pass step 1 before either completes step 2, which produces a **double booking**. This project is about preventing that properly, not hiding it behind a UI.

The engineering focus:

- Transaction design and **pessimistic row locking** in PostgreSQL
- **Database-enforced invariants**, so correctness holds even if application code has a bug
- **Idempotent APIs**, so client retries never create a second reservation
- Safe state transitions between confirm, cancel, and expire when they happen concurrently
- Concurrency tests against a **real PostgreSQL**, not only mocks

This is not a ticket-marketplace project. There are no payments, refunds, or pricing engines.

---

## Tech stack

| Concern | Technology | Why |
| --- | --- | --- |
| Language / framework | Java, Spring Boot | REST API, dependency injection, transaction management |
| Persistence | PostgreSQL, Spring Data JPA / Hibernate | Transactional source of truth, relational constraints |
| Migrations | Flyway | Versioned schema. Hibernate runs with `ddl-auto=validate` and never changes the schema |
| Security | Spring Security, JWT, BCrypt | Authentication, role checks, ownership enforcement |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers | Unit tests and real-database integration tests |
| Build / runtime | Maven, Docker Compose | Reproducible build and local environment |

Redis, RabbitMQ, a transactional outbox, Prometheus/Grafana and k6 are **planned for a later Advanced release** and are not part of the Core release. See [Roadmap](#roadmap).

---

## Architecture

A **modular monolith**: one deployable Spring Boot application, one PostgreSQL database, clear package boundaries.

```
REST API (controllers, validation, OpenAPI)
        |
Spring Boot application
  |-- user         (accounts, roles)
  |-- event        (event lifecycle, cancellation policy)
  |-- resource     (reservable items within an event)
  |-- reservation  (hold, confirm, cancel, expire)
  |-- idempotency  (key scope, payload hash, replay result)
  |-- security / exception / config / common
        |
PostgreSQL  (authoritative state, constraints, transactions, row locks)
```

Package layout:

```
com.malik.reservation
├── config/  security/  exception/  common/
├── user/         (entity, repo, service, controller, dto)
├── event/        (entity, repo, service, controller, dto)
├── resource/     (entity, repo, service)
├── reservation/  (entity, repo, service, controller, dto)
└── idempotency/  (entity, repo, service)
```

Key rules:

- **PostgreSQL decides** whether a resource can be allocated. Nothing else is the authority.
- Business rules live in services, not controllers.
- Entity relationships start **unidirectional** and use `FetchType.LAZY`. Reverse collections are added only when a real use case needs them.

---

## How double booking is prevented

Two independent layers:

**1. Pessimistic row lock (coordination).** Creating a hold runs in one transaction:

1. Lock the target `resources` row.
2. Re-read its state after acquiring the lock.
3. Reject with `409 Conflict` if it is not available.
4. Create the `HELD` reservation with a server-generated expiry.
5. Update the resource state.
6. Store the idempotency result.
7. Commit. Success is returned only after the commit.

**2. Database constraint (defense in depth).** A partial unique index guarantees that at most one active reservation can exist per resource, even if the locking path were bypassed by a bug:

```sql
CREATE UNIQUE INDEX uq_reservations_one_active_per_resource
    ON reservations (resource_id)
    WHERE status IN ('HELD', 'CONFIRMED');
```

The index does not depend on the current time. Expiry is a **persisted state change** (`HELD` to `EXPIRED`) made in a transaction, never inferred from a timestamp.

---

## Data model

Five tables, UUID primary keys generated in Java, `TIMESTAMPTZ` timestamps in UTC.

| Table | Purpose |
| --- | --- |
| `app_users` | Accounts. Unique normalized email, hashed password, role `USER` or `ADMIN` |
| `events` | Name, location, start/end time, status (`DRAFT`, `PUBLISHED`, `CANCELLED`), cancellation cutoff |
| `resources` | A reservable unit in an event. Unique `(event_id, resource_code)`. Status `AVAILABLE`, `HELD`, `BOOKED` |
| `reservations` | One user's claim on one resource. Status `HELD`, `CONFIRMED`, `EXPIRED`, `CANCELLED` |
| `idempotency_records` | Stored result of a successful hold request, unique on `(user_id, operation, key)` |

Design decisions:

- No redundant `event_id` on reservations. The event is reached through `reservation → resource → event`, so there is a single source of truth.
- Reservation history is preserved. Foreign keys use `ON DELETE RESTRICT`.
- Idempotent replays use typed columns, not a raw response blob, and never reconstruct the result from the reservation's current status.

### Reservation lifecycle

```
            confirm (before expiry)
   HELD ───────────────────────────────▶ CONFIRMED
    │  │                                      │
    │  └── expiry worker ──▶ EXPIRED          │ cancel (before cutoff)
    │                                         ▼
    └── cancel ───────────────────────▶ CANCELLED
```

When a resource is released, it is released **only if** the reservation being transitioned is still the one occupying it.

---

## Idempotency

Hold creation requires an `Idempotency-Key` header.

- Same user + operation + key + same payload: returns the original result, creates nothing new.
- Same key + different payload: rejected with a conflict.
- Concurrent duplicates are settled by the database unique constraint, not by an application-level "check then insert".
- Records are kept for a configurable retention period (default 24 hours).

---

## Project status

Legend: ✅ done and verified · 🚧 in progress · 📋 planned

| Area | Status | Notes                                                    |
| --- | --- |----------------------------------------------------------|
| Requirements (SRS v2.0) | ✅ | Core vs Advanced scope agreed                            |
| Architecture and transaction strategy | ✅ | Modular monolith, pessimistic locking, constraint defense |
| ER model and schema design | ✅ | Five tables, all constraints specified                   |
| Flyway migration `V1__create_core_schema.sql` | ✅ | Applied and verified                                     |
| Package structure | ✅ | See above                                                |
| JPA entity mappings and repositories | ✅ | Only currently required                                  |
| Global exception handling and error format | ✅ | Only currently required                                  |
| Security: registration, login, JWT | ✅ | Implemented required only                                |
| Hold creation with row locking | 📋 |                                                          |
| Idempotent hold creation | 📋 |                                                          |
| Confirm and cancel | 📋 |                                                          |
| Expiry worker | 📋 |                                                          |
| Concurrency tests (100 concurrent requests) | 📋 | Results will be recorded here once measured              |
| OpenAPI documentation | 📋 |                                                          |

---

## Getting started

> Commands below follow the standard layout of this repo (Maven wrapper and `compose.yaml`). Confirm them against your own setup and edit if they differ.

**Prerequisites:** Java (the version pinned in `pom.xml`), Docker, and Docker Compose.

```bash
# Start PostgreSQL
docker compose up -d

# Run the application (Flyway applies the schema on startup)
./mvnw spring-boot:run

# Run tests
./mvnw test
```

Configuration lives in `src/main/resources/application.properties`. Secrets such as the JWT signing key and database password must come from environment variables.

---

## API overview (planned)

| Method and path | Access | Purpose |
| --- | --- | --- |
| `POST /api/auth/register` | Public | Register a user |
| `POST /api/auth/login` | Public | Log in and receive a token |
| `GET /api/events` | Public | List published events |
| `GET /api/events/{eventId}/resources` | Public | View resource availability (advisory only) |
| `POST /api/admin/events` | Admin | Create an event |
| `POST /api/admin/events/{eventId}/resources` | Admin | Add resources to an event |
| `POST /api/reservations/holds` | Authenticated | Create a hold (requires `Idempotency-Key`) |
| `POST /api/reservations/{id}/confirm` | Owner | Confirm an unexpired hold |
| `POST /api/reservations/{id}/cancel` | Owner | Cancel an eligible reservation |
| `GET /api/reservations/me` | Authenticated | View own reservation history |

Swagger/OpenAPI location will be added once implemented.

---

## Testing approach

- **Unit tests:** state-transition rules, ownership and role policy, idempotency payload hashing.
- **Integration tests on real PostgreSQL** (Testcontainers): constraints, locking, rollback behavior, security and ownership.
- **Mandatory concurrency tests**, to be run with a synchronization barrier so requests truly contend:
    - 100 concurrent requests, different keys, one resource: exactly 1 active reservation succeeds
    - 100 concurrent retries with the same key and payload: exactly 1 logical hold
    - Confirm racing with expiry: exactly one transition wins
    - Cancel racing with a new hold: no duplicate active allocation

Results will be recorded in this README only after they have actually been run. No performance numbers will be published without a documented measurement and environment.

---

## Roadmap

**Core release (current focus):** authentication, events and resources, hold / confirm / cancel / expire, idempotency, Flyway, automated tests, Docker Compose, OpenAPI.

**Advanced release (only after Core is correct and tested):** Redis (read cache and rate limiting), RabbitMQ with a transactional outbox, retries and dead-letter queue, Prometheus and Grafana metrics, k6 load tests.

**Out of scope:** payments and refunds, microservices, Kubernetes, Kafka, Elasticsearch, exactly-once message delivery.

---

## Author

Malik Nadeem · [GitHub](https://github.com/Malik-AK47) · [LinkedIn](https://www.linkedin.com/in/malik-nadeem47/)