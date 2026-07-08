# MissionMatch

**MissionMatch** is a reference application for freelance mission sourcing and matching. A freelancer's profile (skills, daily rate, availability) is automatically matched against incoming missions, and every match is tracked through an application pipeline until it turns into a signed contract - or a rejection.

Beyond its business purpose, this project exists as a **learning vehicle**. It is deliberately over-engineered for its size so that every layer demonstrates a specific professional practice in isolation:

| Practice | Where it shows up |
|---|---|
| Domain-Driven Design (DDD) | Bounded contexts, aggregates, ubiquitous language |
| Hexagonal Architecture (Ports & Adapters) | Every backend module |
| Test-Driven Development (TDD) | Domain and application layers, written test-first |
| Behavior-Driven Development (BDD) | `// Given // When // Then` test structure, acceptance scenarios |
| Event-driven architecture | Kafka topics between bounded contexts |
| Infrastructure as Code | Terraform modules for AWS |

If you are reading this to learn the practices rather than to use the app, read the [Glossary](#glossary) first - it defines every term used below in plain language. For a much deeper, fully pedagogical walkthrough of every concept with real code excerpts (available in English and French), see **[docs/en/ARCHITECTURE.md](docs/en/ARCHITECTURE.md)** / **[docs/fr/ARCHITECTURE.md](docs/fr/ARCHITECTURE.md)**.

---

## Table of contents

1. [The business problem](#the-business-problem)
2. [Domain model & bounded contexts](#domain-model--bounded-contexts)
3. [Architecture: Hexagonal (Ports & Adapters)](#architecture-hexagonal-ports--adapters)
4. [Event-driven communication](#event-driven-communication)
5. [Tech stack](#tech-stack)
6. [Repository structure](#repository-structure)
7. [Testing strategy](#testing-strategy)
8. [REST API overview](#rest-api-overview)
9. [Running the project locally](#running-the-project-locally)
10. [Infrastructure & deployment (AWS + Terraform)](#infrastructure--deployment-aws--terraform)
11. [Coding conventions](#coding-conventions)
12. [Roadmap](#roadmap)
13. [Glossary](#glossary)

---

## The business problem

A freelancer receives mission opportunities from many sources (job boards, referrals, direct client contact). Manually checking whether a mission fits their skills, rate expectations and availability - and then tracking every application through interviews to a signed contract - does not scale. MissionMatch automates the matching and gives visibility over the whole pipeline, from "mission published" to "contract signed".

The workflow in one sentence: **a mission is published → it is scored against the freelancer's profile → good matches become candidature suggestions → the freelancer moves them through a pipeline → everyone gets notified along the way.**

---

## Domain model & bounded contexts

DDD starts from the idea that a large domain cannot be modeled with one single, consistent model - different parts of the business use the same words to mean different things, and trying to unify them produces a bloated, contradictory model. Instead, DDD splits the domain into **bounded contexts**: each one owns a small, internally consistent model and a precise vocabulary (the **ubiquitous language**) that the code speaks literally - class and method names should read like a domain expert talking, not like database rows.

MissionMatch has five bounded contexts:

| Context | Aggregate root | Owns | Publishes | Listens to |
|---|---|---|---|---|
| **Sourcing** | `Mission` | Mission lifecycle: created, open, closed | `MissionPublished`, `MissionClosed` | - |
| **FreelancerProfile** | `Profile` | Skills, expected daily rate | `ProfileUpdated` | - |
| **Matching** | `MatchResult` | Scoring algorithm, match history | `MatchComputed` | `MissionPublished`, `MissionClosed`, `ProfileUpdated` |
| **ApplicationTracking** | `Candidature` | Pipeline status (kanban) | `CandidatureStatusChanged` | `MatchComputed` |
| **Notification** | *(no aggregate - a "leaf" context)* | Email/Slack delivery | - | `MatchComputed`, `CandidatureStatusChanged` |

### Why these boundaries?

- **Sourcing** and **FreelancerProfile** know nothing about each other. A mission is a valid mission whether or not a matching engine exists. This independence is what makes them safe to change without a ripple effect.
- **Matching** is the only context that needs to know about both a mission and a profile at the same time - that reasoning is quarantined here instead of leaking into Sourcing or Profile.
- **ApplicationTracking** doesn't recompute anything; it only reacts to a match by offering the freelancer a candidature to act on. The pipeline status (`TO_APPLY → APPLIED → INTERVIEW → REJECTED/ACCEPTED`) is *its* concept, not the Matching context's.
- **Notification** has no aggregate at all because it has no state worth protecting - it is a pure side-effect executor. Not every context needs the full DDD tactical toolkit.

### Context map

```
 Sourcing ────MissionPublished────┐
                                   ▼
FreelancerProfile ──ProfileUpdated──▶  Matching  ──MatchComputed──▶ ApplicationTracking
                                   │                      │
                                   └──────────┬───────────┘
                                              ▼
                                        Notification
```

Arrows are Kafka events, not method calls - no context imports another context's domain classes. This is what keeps a "modular monolith" from silently turning into a big ball of mud: the module boundary is enforced by the fact that the *only* legal way to talk to another context is to publish or consume an event (or, for synchronous reads, to go through its REST API - never through its database or internal classes).

---

## Architecture: Hexagonal (Ports & Adapters)

Hexagonal architecture (also called Ports & Adapters, coined by Alistair Cockburn) solves one problem: **keep business logic ignorant of technology**. A `Mission` aggregate should not know it is persisted in Postgres via JPA, and a matching algorithm should not know it is triggered by a Kafka message or an HTTP call. If it did, every framework upgrade or infrastructure change would risk breaking business rules, and testing business rules would require spinning up that infrastructure.

Every module is split into three concentric layers:

```
                 ┌─────────────────────────────────────┐
                 │           infrastructure              │
                 │  (adapters: web, Kafka, JPA, email)   │
                 │   ┌───────────────────────────────┐   │
                 │   │          application           │   │
                 │   │   (use cases, ports in/out)    │   │
                 │   │   ┌─────────────────────────┐  │   │
                 │   │   │        domain            │  │   │
                 │   │   │ (aggregates, VOs, rules)  │  │   │
                 │   │   └─────────────────────────┘  │   │
                 │   └───────────────────────────────┘   │
                 └─────────────────────────────────────┘
```

- **`domain`** - pure Kotlin, zero framework imports. Aggregates, value objects, domain events and domain services live here. This code should compile even if you deleted Spring Boot from the classpath.
- **`application`** - orchestrates domain objects to fulfill a use case (e.g. "compute a match"). It defines two kinds of **ports** (interfaces):
  - **Input ports** (`application/port/input`): what the outside world can ask this module to do (e.g. `HandleMissionPublishedUseCase`). Driving adapters call these. (Named `input`, not `in` - `in` is a reserved keyword in Kotlin.)
  - **Output ports** (`application/port/output`): what this module needs from the outside world (e.g. `MatchResultRepository`, `MatchEventPublisher`). Driven adapters implement these.
- **`infrastructure`** - the only layer allowed to depend on frameworks. **Driving adapters** (REST controllers, Kafka consumers) call into input ports. **Driven adapters** (JPA repositories, Kafka producers, email senders) implement output ports.

The dependency rule is: **arrows only point inward.** `infrastructure` depends on `application`, `application` depends on `domain`, and `domain` depends on nothing. This is what lets you unit-test the domain and application layers with zero mocks of infrastructure - you mock the *port interface*, which is a plain Kotlin interface you own, not a database or a broker.

The `matching` module, as it actually exists in the repo:

```
matching/
├── domain/
│   ├── MatchResult.kt                          # aggregate
│   ├── MatchingScore.kt                        # value object
│   └── MatchingPolicy.kt                       # domain service: the actual scoring rules
├── application/
│   ├── port/input/HandleMissionPublishedUseCase.kt
│   ├── port/input/HandleProfileUpdatedUseCase.kt
│   ├── port/input/GetMatchesUseCase.kt
│   ├── port/output/MissionSnapshotRepository.kt
│   ├── port/output/ProfileSnapshotRepository.kt
│   ├── port/output/MatchResultRepository.kt
│   ├── port/output/MatchEventPublisher.kt
│   └── MatchingApplicationService.kt           # implements all three input ports
└── infrastructure/
    ├── adapter/input/messaging/MissionPublishedConsumer.kt
    ├── adapter/input/messaging/ProfileUpdatedConsumer.kt
    ├── adapter/input/web/MatchController.kt
    ├── adapter/output/persistence/MatchResultRepositoryAdapter.kt
    └── adapter/output/messaging/KafkaMatchEventPublisher.kt
```

Note that `MissionPublishedConsumer` (Kafka), `ProfileUpdatedConsumer` (Kafka) and `MatchController` (REST) are three different adapters all calling into the *same* application service. That's the point of the pattern: the business rule for computing a match doesn't change depending on who's asking.

---

## Event-driven communication

Contexts communicate exclusively through **domain events** published to Kafka, using a **choreography** style: each context reacts to events it cares about and publishes its own, with no central conductor deciding "what happens next". This mirrors how the real business works - Sourcing doesn't need to know that publishing a mission will eventually trigger a Slack notification three contexts away.

| Topic | Producer | Consumers | Payload (essentials) |
|---|---|---|---|
| `mission-published` | Sourcing | Matching | missionId, requiredSkills, dailyRate, startDate |
| `mission-closed` | Sourcing | Matching | missionId |
| `profile-updated` | FreelancerProfile | Matching | freelancerId, skills, expectedDailyRate |
| `match-computed` | Matching | ApplicationTracking, Notification | missionId, freelancerId, score |
| `candidature-status-changed` | ApplicationTracking | Notification | candidatureId, previousStatus, newStatus |

Why events instead of direct REST calls between contexts? Two reasons this project exists to demonstrate:

1. **Temporal decoupling** - Matching doesn't need Sourcing to be online to eventually process a mission; it consumes at its own pace.
2. **Multiple consumers, one producer** - `match-computed` has two independent consumers today (ApplicationTracking, Notification) and could gain a third tomorrow (e.g. an analytics context) without ApplicationTracking's producer ever knowing or changing.

The tradeoff, and the reason this is worth learning deliberately rather than defaulting to REST everywhere: eventual consistency. A freelancer might see a mission in Sourcing for a few hundred milliseconds before a match appears - the UI must be built to tolerate that, not assume synchronous consistency.

Four consequences of choreography and at-least-once delivery that this project ran into for real (not hypothetically) while building it:

- **Every context's view of the world can silently drift from the source of truth** if it doesn't subscribe to the events that keep it current. Matching originally only consumed `MissionPublished` and `ProfileUpdated` - a mission closed in Sourcing stayed "open" in Matching's own read model and could still surface as a fresh match. Fixed by also consuming `MissionClosed`.
- **Different topics have no ordering guarantee relative to each other, even when one event logically depends on another.** Fixing the gap above by naively consuming `MissionClosed` wasn't enough on its own: `mission-published` and `mission-closed` are different topics, so `MissionClosed` can be consumed *before* `MissionPublished` has created the snapshot it's supposed to close, since each topic-partition's consumer becomes ready independently. The fix isn't "wait a bit" - it's a small `closed_mission_markers` table Matching writes to unconditionally on `MissionClosed`, regardless of whether a snapshot exists yet; `MissionPublished`'s handler checks it before deciding whether the mission it's about to create a snapshot for should start out open or already closed. Correct regardless of which event arrives first.
- **Kafka's at-least-once delivery means consumers must be idempotent.** Reprocessing the same event (a consumer rebalance is enough to trigger it) must not corrupt state. `match_results` has a unique constraint on `(mission_id, freelancer_id)`, and the repository adapter treats a constraint violation as "someone else already recorded this" rather than an error - discovered by running the real system with real Kafka, not by reasoning about it in the abstract.
- **A shared global type mapping can't serve two consumers of the same topic that each want their own DTO.** Once `match-computed` genuinely had two independent consumers (ApplicationTracking and Notification, each with its own anti-corruption event class), Spring's single `spring.json.type.mapping` property could only point the alias at one of them. The fix is a dedicated `ConcurrentKafkaListenerContainerFactory` per consumer, each with its own `JsonDeserializer` bound to its own target class - Kafka doesn't require every consumer group reading a topic to deserialize it the same way, since deserialization is a client-side concern per consumer group, not a property of the topic itself.

---

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Backend language | Kotlin | Null-safety and concise data classes suit value objects/aggregates well |
| Backend framework | Spring Boot | Dependency injection wires adapters to ports without the domain knowing |
| Messaging | Apache Kafka (Amazon MSK Serverless in AWS) | Durable, replayable event log - fits choreography naturally |
| Persistence | PostgreSQL (via Spring Data JPA) | One instance, one schema per bounded context |
| Frontend | Angular (standalone components, signals) | Missions, Profile and Matches pages today; `application-tracking`'s API exists but has no UI yet |
| Unit/integration testing | JUnit 5, Mockito, AssertJ | See [Testing strategy](#testing-strategy) |
| Integration testing infra | Testcontainers (Postgres, Kafka) | Tests run against real engines, not mocks, without needing shared infra |
| Infrastructure as Code | Terraform | AWS provisioning, versioned and reviewable like code |

---

## Repository structure

```
missionmatch/
├── backend/
│   ├── settings.gradle.kts
│   ├── shared-kernel/          # cross-context value objects & event envelope (SkillSet, Money, EventMetadata)
│   ├── sourcing/                       # fully wired: domain, application, infrastructure
│   ├── freelancer-profile/             # fully wired: domain, application, infrastructure
│   ├── matching/                       # fully wired: domain, application, infrastructure
│   ├── application-tracking/           # fully wired: domain, application, infrastructure
│   ├── notification/                   # fully wired: no aggregate, no persistence, no REST
│   └── bootstrap/              # the single deployable Spring Boot app wiring every module together
├── frontend/
│   └── src/app/
│       ├── missions/            # publish/list/close missions
│       ├── profile/             # create/update the local freelancer's profile
│       ├── matches/             # look up matches by freelancer id
│       ├── candidatures/        # candidature pipeline kanban, by freelancer id
│       └── shared/              # sidebar, chip-input, status-badge, tag colors, ...
├── docker-compose.yml           # Postgres + Kafka for local development
├── docs/
│   ├── en/ARCHITECTURE.md       # full pedagogical guide, English
│   └── fr/ARCHITECTURE.md       # full pedagogical guide, French
└── infra/terraform/             # written and validated, not yet applied - see infra/terraform/README.md
    ├── modules/{network,ecs-service,rds,msk,frontend-hosting,observability}/
    ├── environments/{dev,prod}/     # backend.tf (partial S3 config) + backend.hcl per environment
    └── bootstrap/                   # one-time: creates the S3 state bucket + DynamoDB lock table
```

`shared-kernel` is intentionally tiny. In DDD, a **shared kernel** is a piece of model that multiple contexts agree to share - it should stay small and stable, because every context depending on it now has to agree before it changes. Here it only holds truly universal, stable concepts (a `Money` value object, an event envelope with correlation IDs) - never a `Mission` or a `Profile`.

### Monolith first, microservices later

MissionMatch ships as **one deployable Spring Boot application** (a *modular monolith*): every bounded context is its own Gradle module with its own hexagonal layers, but they all run in the same process and the same ECS service. This is a deliberate choice for a learning/reference project - it gives full practice with DDD boundaries and real Kafka messaging without the operational cost of deploying, monitoring and versioning five separate services.

The module boundaries are strict enough (no cross-module imports of `domain` or `application` classes, communication only via Kafka events or REST) that splitting any module into its own microservice later is a deployment change, not a redesign.

---

## Testing strategy

This project treats testing as a first-class design activity, following the **testing pyramid**: many fast, isolated tests at the bottom, fewer slow, broad tests at the top.

### Domain layer - pure unit tests
Aggregates and value objects are plain Kotlin with business rules (e.g. "a match score above 0.7 is eligible"). They're tested with **JUnit 5** and **AssertJ**, with no mocks at all - a pure function in, an assertion on the result out. This is where **TDD** (Test-Driven Development: write the failing test first, then the minimal code to pass it, then refactor) is easiest and most valuable, because feedback is instantaneous.

```kotlin
@Test
fun `a published mission is open and eligible for matching`() {
    // Given
    val mission = Mission.publish(
        title = "Kotlin backend developer",
        clientName = "Acme Corp",
        requiredSkills = SkillSet.of("kotlin", "spring"),
        dailyRate = Money.of(600.0),
        startDate = LocalDate.now().plusWeeks(2),
    )

    // When / Then
    assertThat(mission.status).isEqualTo(MissionStatus.OPEN)
    assertThat(mission.isEligibleForMatching()).isTrue()
}
```

### Application layer - orchestration tests
Use cases (application services) are tested with **JUnit 5 + Mockito**, mocking the *output ports* only (never the domain). This verifies the use case calls the right ports in the right order - e.g. "compute the score, and if it's above threshold, publish `MatchComputed`" - without touching a real database or broker.

```kotlin
@Test
fun `a below-threshold score is neither persisted nor published`() {
    // Given
    val nonMatchingProfile = ProfileSnapshot(
        freelancerId = FreelancerId(UUID.randomUUID()),
        skills = SkillSet.of("php"),
        expectedDailyRate = Money.of(500.0),
    )
    whenever(profileSnapshotRepository.findAll()).thenReturn(listOf(nonMatchingProfile))
    val command = MissionPublishedCommand(/* ... */)

    // When
    service.handle(command)

    // Then
    verify(matchResultRepository, never()).save(any())
    verify(matchEventPublisher, never()).publish(any())
}
```

### Infrastructure layer - real-engine integration tests
Adapters are tested against **real** Postgres and Kafka using **Testcontainers**, which spins up throwaway Docker containers for the test run. This catches the class of bug unit tests structurally cannot: a wrong SQL mapping, a serialization mismatch on a Kafka topic, a misconfigured index. No mocking a database driver - the point is to trust the adapter because it ran against the real thing.

```kotlin
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MissionRepositoryAdapterTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Test
    fun `persists a mission and retrieves it back by id`() {
        // Given
        val mission = Mission.publish(/* ... */)

        // When
        repository.save(mission)
        val found = repository.findById(mission.id)

        // Then
        assertThat(found?.title).isEqualTo(mission.title)
    }
}
```

### Acceptance tests - BDD
Every test in the codebase, at every layer, follows the **`// Given // When // Then`** structure (Behavior-Driven Development's core idea: describe behavior from an observer's point of view - starting state, action, observable outcome - rather than describing implementation steps). For end-to-end scenarios that matter to the business ("a freelancer applies to a matched mission and sees it move to the interview stage"), these three sections map directly onto a Gherkin scenario if the project later adopts Cucumber for a living-documentation suite - but plain JUnit with the comment convention is enough to get the same clarity benefit without extra tooling.

| Layer | Tooling | What's real, what's faked |
|---|---|---|
| Domain | JUnit 5, AssertJ | Everything real - no infrastructure exists yet |
| Application | JUnit 5, Mockito, AssertJ | Domain real, ports mocked |
| Infrastructure adapters | JUnit 5, Testcontainers, AssertJ | Database/broker real (containerized), nothing mocked |
| Acceptance (optional) | Cucumber or plain JUnit | Full stack, `Given/When/Then` as scenario steps |

---

## REST API overview

Each context exposes its own REST resource under an `/api` prefix; there is no API gateway in this reference project (one could be added as a Terraform/infra concern without touching the backend code). The `/api` prefix exists specifically so the Angular frontend's own client-side routes (`/missions`, `/matches`, ...) never collide with a backend path when both are served from the same origin through the dev-server proxy or a production reverse proxy.

| Method | Path | Context | Purpose |
|---|---|---|---|
| `POST` | `/api/missions` | Sourcing | Publish a new mission |
| `GET` | `/api/missions` | Sourcing | List all missions |
| `GET` | `/api/missions/{id}` | Sourcing | Fetch mission details |
| `POST` | `/api/missions/{id}/close` | Sourcing | Close a mission |
| `PUT` | `/api/profile/{freelancerId}` | FreelancerProfile | Create or update a profile (skills, expected daily rate) |
| `GET` | `/api/profile/{freelancerId}` | FreelancerProfile | Fetch a profile |
| `GET` | `/api/matches?freelancerId=` | Matching | List matches for a freelancer |
| `GET` | `/api/candidatures?freelancerId=` | ApplicationTracking | List pipeline entries |
| `PATCH` | `/api/candidatures/{id}/status` | ApplicationTracking | Move a candidature to another pipeline stage |

Every controller throws plain domain exceptions (`IllegalArgumentException`/`IllegalStateException` for invalid input or a broken invariant, `NoSuchElementException` for an unknown id) and never touches HTTP status codes itself. A single `@RestControllerAdvice` in `bootstrap` (the composition root, the only place that knows about every controller) maps those to `400`/`404` with a small `{"message": "..."}` body, application-wide - one cross-cutting concern, one place, instead of duplicated try/catch in every context's web adapter.

---

## Running the project locally

```bash
# Start Postgres + Kafka locally
docker compose up -d

# Run the backend (all modules, one process)
cd backend && ./gradlew :bootstrap:bootRun

# Run the frontend
cd frontend && npm start
```

Ports are deliberately non-default to avoid clashing with other projects running on the same machine: backend API on `8181`, frontend dev server on `4310`, Postgres on `5442`, Kafka on `9192`. The frontend's dev-server proxy (`proxy.conf.json`) already points at `8181`, so `npm start` works out of the box once the backend is up.

A `docker-compose.yml` at the repo root provisions Postgres and a single-broker Kafka, matching what Testcontainers spins up in tests - the goal is that "it passed the integration tests" and "it works locally" rely on the same infrastructure shape.

### Seeing it with real data

An empty app is a poor demo. Run the backend with the `demo` Spring profile and a `DemoDataSeeder` publishes six realistic missions (two of them closed) and creates a matching freelancer profile through the real REST-and-Kafka path, exactly like a user would:

```bash
cd backend && ./gradlew :bootstrap:bootRun --args='--spring.profiles.active=demo'
```

The seeded freelancer id is `11111111-1111-1111-1111-111111111111` if you want to look it up directly; visiting the Matches page in a fresh browser will instead show *your own* browser's (empty, until you fill in the Profile page) local identity.

---

## Infrastructure & deployment (AWS + Terraform)

Terraform describes every AWS resource this application needs, split into reusable modules composed differently per environment.

| Module | Resource(s) | Purpose |
|---|---|---|
| `network` | VPC, public/private subnets, NAT, IGW | Isolates the app; only the ALB is internet-facing |
| `ecs-service` | ECS Fargate service, ALB, target group | Runs the Spring Boot container |
| `rds` | RDS PostgreSQL instance | One instance, one schema per bounded context |
| `msk` | Amazon MSK Serverless | Managed Kafka - no broker operations to run by hand |
| `frontend-hosting` | S3 + CloudFront + Route53 + ACM | Serves the built Angular app over HTTPS |
| `observability` | CloudWatch log groups & alarms | CPU/memory on ECS, 5xx rate on the ALB |

```
infra/terraform/
├── modules/            # reusable building blocks, one concern each
├── environments/
│   ├── dev/            # composes the modules for a cheap, always-on dev stack
│   └── prod/           # same modules, sized/hardened differently
├── bootstrap/          # one-time: creates the S3 state bucket + DynamoDB lock table
└── README.md           # how to actually run any of this
```

State is remote (S3 + DynamoDB lock) from day one, even for a solo project - it's the practice that matters, and it removes "works on my machine" from infrastructure changes. Each environment's `backend.tf` is a partial config; the real bucket/key/region live in that environment's `backend.hcl`, applied with `terraform init -backend-config=backend.hcl`. The plan is for CI to run `terraform plan` on every pull request and `terraform apply` on merge to `main` (with manual approval before `prod` is introduced) - not built yet, see Roadmap.

Secrets (DB credentials, Kafka auth) live in AWS Secrets Manager and are injected into the ECS task at runtime - never committed, never baked into the container image.

All six modules and both environments are written and pass `terraform validate`, but nothing has been `apply`'d - see [infra/terraform/README.md](infra/terraform/README.md) for the deploy flow and the short list of what it doesn't cover yet (CI/CD, a real domain, and MSK IAM auth exercised against a live cluster rather than just code review).

---

## Coding conventions

- All code, comments and commit messages are in **English**.
- Comments explain **why**, not what - the code should already say what it does through naming.
- Every test follows the `// Given // When // Then` structure, regardless of layer.
- No cross-context imports of `domain` or `application` classes - the compiler should make a boundary violation impossible, not just discouraged by convention.
- Ports are interfaces owned by `application`; adapters live in `infrastructure` and never the reverse.

---

## Roadmap

- [x] Scaffold Gradle multi-module backend with the `sourcing` context fully wired (domain → application → infrastructure)
- [x] Add `docker-compose.yml` (Postgres + Kafka) for local development
- [x] Add the Matching scoring algorithm with full TDD test suite
- [x] Wire `freelancer-profile` end to end and publish real `ProfileUpdated` events
- [x] Angular dashboard: mission list + publish form, match lookup by freelancer id
- [x] Angular: profile page (skills + expected daily rate, backed by a per-browser local identity since there's no auth yet)
- [x] Have Matching consume `MissionClosed` so its read model doesn't drift from Sourcing's
- [x] Wire `application-tracking` end to end (candidature pipeline, consumes `MatchComputed`)
- [x] Wire `notification` (logs instead of real email/Slack, but consumes both `MatchComputed` and `CandidatureStatusChanged` for real) - all five bounded contexts are now fully implemented
- [x] Angular: application pipeline kanban for the candidature statuses `application-tracking` already tracks
- [ ] Swap `LoggingNotificationSender` for a real email/Slack adapter behind the same `NotificationSender` port
- [x] Terraform: all six modules and both environments written, `terraform validate`-clean (see [infra/terraform/README.md](infra/terraform/README.md))
- [ ] Terraform `dev` environment, actually `apply`'d to a real AWS account
- [ ] CI/CD: build the Docker image, push to ECR, run `terraform plan`/`apply`
- [ ] Optional: introduce Cucumber for living-documentation acceptance tests
- [ ] Optional: extract one context (e.g. Notification) into its own microservice, as a worked example of the monolith-to-microservice split

---

## Glossary

- **Bounded context** - a boundary within which a specific domain model and vocabulary is consistent and unambiguous. The same word (e.g. "Profile") can mean something different in another context.
- **Ubiquitous language** - the vocabulary shared between developers and domain experts *within a bounded context*, used literally in code (class names, method names).
- **Aggregate** - a cluster of domain objects treated as a single unit for data changes, with one **aggregate root** as its only entry point (e.g. `Mission`). Guarantees business invariants are never left inconsistent.
- **Entity** - an object defined by its identity (an ID), not its attributes, and whose state can change over time (e.g. a `Candidature`).
- **Value object** - an object defined entirely by its attributes, immutable, with no identity (e.g. `Money`, `SkillSet`). Two value objects with the same data are interchangeable.
- **Domain event** - a fact that happened in the domain, named in the past tense (`MissionPublished`), that other parts of the system may react to.
- **Domain service** - domain logic that doesn't naturally belong to one entity or value object (e.g. `MatchingPolicy`, which needs both a mission and a profile).
- **Shared kernel** - a small piece of model explicitly shared between bounded contexts, kept intentionally minimal because every dependent context must agree on changes to it.
- **Port** - an interface, owned by the application layer, describing a capability needed (output port) or offered (input port) by the module - independent of any specific technology.
- **Adapter** - a concrete implementation of a port using a specific technology (a JPA repository, a Kafka consumer, a REST controller).
- **Driving adapter** - an adapter that initiates a call into the application (e.g. a REST controller, a Kafka consumer reacting to a message).
- **Driven adapter** - an adapter the application calls out to (e.g. a JPA repository, a Kafka producer).
- **Choreography** (vs. orchestration) - an integration style where each service reacts independently to events with no central coordinator deciding the sequence, as opposed to orchestration, where a central process explicitly calls each step.
- **Eventual consistency** - the guarantee that, absent new updates, all parts of a distributed system will *eventually* reflect the same state - but not necessarily at the same instant.
- **TDD (Test-Driven Development)** - writing a failing test before the production code that makes it pass, then refactoring, in short repeated cycles.
- **BDD (Behavior-Driven Development)** - describing and testing behavior from an observable, business-readable point of view (Given/When/Then) rather than from implementation details.
- **Testcontainers** - a library that runs real dependencies (databases, brokers) as throwaway Docker containers during tests, so integration tests exercise the real engine instead of a mock or an in-memory substitute.

---

Copyright (c) 2026 Riadh Mnasri. All rights reserved.
