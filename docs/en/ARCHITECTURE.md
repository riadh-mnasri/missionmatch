# MissionMatch - Complete Architecture & Concepts Guide

*[Lire en français](../fr/ARCHITECTURE.md)*

This document is the deep, pedagogical companion to the project [README](../../README.md). The README is a reference: it tells you what exists and how to run it. This document is a *guide*: it teaches every concept the codebase demonstrates, one at a time, always anchored to real code from this repository - never invented examples.

If you already know DDD, hexagonal architecture, and event-driven systems cold, you don't need this document - the README is enough. If any of those topics are new to you, or you want to see how the textbook definitions turn into actual working Kotlin and TypeScript, read on. Every section follows the same shape: **what the concept is**, **why it exists (what problem it solves)**, and **where it lives in this codebase**, with real excerpts.

## Table of contents

1. [How to read this project](#1-how-to-read-this-project)
2. [The business problem, in plain terms](#2-the-business-problem-in-plain-terms)
3. [Domain-Driven Design](#3-domain-driven-design)
4. [Hexagonal Architecture - a complete walkthrough](#4-hexagonal-architecture--a-complete-walkthrough)
5. [Event-driven architecture with Kafka](#5-event-driven-architecture-with-kafka)
6. [War stories: real distributed-systems bugs this project hit](#6-war-stories-real-distributed-systems-bugs-this-project-hit)
7. [Test-Driven Development & Behavior-Driven Development](#7-test-driven-development--behavior-driven-development)
8. [A guided tour of the code structure](#8-a-guided-tour-of-the-code-structure)
9. [The frontend, concept by concept](#9-the-frontend-concept-by-concept)
10. [Infrastructure as code: from six modules to a running system](#10-infrastructure-as-code-from-six-modules-to-a-running-system)
11. [Glossary](#11-glossary)

---

## 1. How to read this project

MissionMatch is a small freelance-mission-matching application, but it is *deliberately* built the way a much bigger, longer-lived system would be built, so that every professional practice it demonstrates can be seen in isolation, in real running code, rather than described in the abstract.

The practical consequence: don't judge the amount of ceremony (interfaces for everything, a module per bounded context, anti-corruption DTOs that look redundant) by the size of the feature it supports. A "publish a mission" feature that could be a single 20-line script anywhere else takes a dozen files here **on purpose** - each file is a worked example of one architectural idea.

The best way to use this guide is side by side with the actual source tree. Every code excerpt below is copy-pasted from the repository, not simplified or invented, so you can open the real file and see it in full context.

---

## 2. The business problem, in plain terms

A freelancer hears about mission opportunities from many places. For each one, they have to manually check: *do I have the right skills? Does the rate work for me? Am I available?* Then, if it looks promising, they apply and track that application through interviews until it becomes a contract, or doesn't.

MissionMatch automates the middle step - the matching - and gives visibility over the pipeline:

```
a mission is published
        │
        ▼
it is scored against freelancer profiles
        │
        ▼
good matches become candidature suggestions
        │
        ▼
the freelancer moves them through a pipeline
        │
        ▼
everyone gets notified along the way
```

Each arrow in that diagram is, not coincidentally, a **bounded context boundary** in the code. That's the first concept worth understanding properly.

---

## 3. Domain-Driven Design

### 3.1 The problem DDD solves

Imagine building MissionMatch as one big model: a single `Mission` class, a single `User` class, one big service layer. At first this is fine. But "mission" means something subtly different depending on who's talking about it: to the person sourcing missions, a mission is a lifecycle (draft → open → closed). To the matching engine, a mission is just a bag of required skills and a rate, used for scoring. To a future billing feature, a mission would be a line item on an invoice. Force all of that into one `Mission` class and you get a bloated object with fields that only make sense to some of its callers, and business rules that contradict each other depending on which feature touched the class last.

**Domain-Driven Design's answer:** don't build one model. Split the domain into **bounded contexts**, each with its own small, internally consistent model, and let each context define "Mission" (or whatever concept) exactly as *it* needs to see it, no more.

### 3.2 Ubiquitous language

Inside a bounded context, the code should read like a domain expert talking, not like database rows. In this repo, that means: no `MissionDTO`, no `MissionEntity` leaking into the domain layer, no `getMissionData()`. Instead:

```kotlin
// backend/sourcing/.../domain/Mission.kt
fun isEligibleForMatching(): Boolean = status == MissionStatus.OPEN

fun close() {
    check(status == MissionStatus.OPEN) { "Only an open mission can be closed" }
    status = MissionStatus.CLOSED
}
```

`isEligibleForMatching()` and `close()` are words a product manager would use. That's the ubiquitous language at work: the vocabulary is shared between the code and the people who understand the business, and it's *literally* the method names, not a comment translating technical jargon into business terms.

### 3.3 Bounded contexts in MissionMatch

MissionMatch has five bounded contexts:

| Context | Aggregate root | Owns | Publishes | Listens to |
|---|---|---|---|---|
| **Sourcing** | `Mission` | Mission lifecycle: created, open, closed | `MissionPublished`, `MissionClosed` | - |
| **FreelancerProfile** | `Profile` | Skills, expected daily rate | `ProfileUpdated` | - |
| **Matching** | `MatchResult` | Scoring algorithm, match history | `MatchComputed` | `MissionPublished`, `MissionClosed`, `ProfileUpdated` |
| **ApplicationTracking** | `Candidature` | Pipeline status (kanban) | `CandidatureStatusChanged` | `MatchComputed` |
| **Notification** | *(no aggregate)* | Email/Slack delivery | - | `MatchComputed`, `CandidatureStatusChanged` |

Why exactly these boundaries, and not, say, one giant "Missions" context?

- **Sourcing** and **FreelancerProfile** know nothing about each other. A mission is a perfectly valid mission whether or not a matching engine exists. Keeping them apart means you can change how missions are sourced without ever touching profile code, and vice versa.
- **Matching** is the *only* context that needs to reason about a mission and a profile at the same time. That reasoning - the scoring algorithm - is quarantined here. If it leaked into Sourcing (e.g. `Mission.computeMatchScore(profile)`), Sourcing would suddenly depend on FreelancerProfile's concepts for no reason intrinsic to sourcing a mission.
- **ApplicationTracking** doesn't recompute anything. It reacts to a match by offering the freelancer something to act on. Its pipeline status (`TO_APPLY → APPLIED → INTERVIEW → REJECTED/ACCEPTED`) is *its own concept* - Matching has no opinion about candidature pipelines, and shouldn't.
- **Notification** has no aggregate at all, because it protects no state worth its own consistency rules. It's a pure side-effect executor: an event comes in, an email goes out. Not every context needs the full DDD tactical toolkit (aggregates, invariants) - knowing when *not* to use a pattern is as important as knowing how to use it.

### 3.4 The context map

```
 Sourcing ────MissionPublished, MissionClosed────┐
                                                   ▼
FreelancerProfile ──ProfileUpdated──▶  Matching  ──MatchComputed──▶ ApplicationTracking
                                                   │                       │
                                                   └───────────┬───────────┘
                                                                ▼
                                                          Notification
```

The arrows are **Kafka events**, never method calls. No context's code imports another context's domain classes. Section 5 explains exactly how that rule is *enforced*, not just followed by convention.

### 3.5 Aggregates, Entities, and Value Objects

These three building blocks show up constantly in the domain layer, and it's worth being precise about what each one is, because they solve different problems.

**Entity** - an object defined by its *identity*, not its current attribute values. Two entities with identical attributes but different IDs are different things. `Mission` is an entity: two missions could have the exact same title, client, skills and rate, and still be two different missions, distinguished only by `MissionId`.

**Value object** - an object defined entirely by its attributes, with no identity of its own. Two value objects holding the same data *are* the same value, interchangeably. `Money` and `SkillSet` (in `shared-kernel`) are value objects:

```kotlin
// backend/shared-kernel/.../domain/Money.kt
data class Money(val amount: BigDecimal, val currency: String = "EUR") {
    init {
        require(amount >= BigDecimal.ZERO) { "Money amount cannot be negative" }
        require(currency.length == 3) { "Currency must be a 3-letter ISO code" }
    }
    // ...
}
```

Two things worth noticing here that are true of *every* value object in this codebase:
1. It's a Kotlin `data class`, which gives structural equality for free (`Money(100, "EUR") == Money(100, "EUR")` is `true` even though they're different object instances) - exactly the "same data = same value" semantics a value object needs.
2. Its `init` block makes it **impossible to construct an invalid `Money`**. You cannot have a negative amount or a malformed currency anywhere in the system, because there is no code path that produces a `Money` instance without passing through this check. This is a core DDD idea: push validation into the type itself so "invalid state" isn't just discouraged, it's *unrepresentable*.

**Aggregate** - a cluster of entities and value objects treated as one unit for the purpose of data changes, with a single **aggregate root** as its only entry point. The rule: nothing outside the aggregate is allowed to reach into it and mutate its internals directly - every change goes through the root, so the root can enforce whatever invariants matter.

`Mission` is an aggregate root (the whole aggregate here is small - just the root plus value objects, no child entities, which is common and fine). Look at how it protects its own consistency:

```kotlin
class Mission private constructor(
    val id: MissionId,
    val title: String,
    // ...
    status: MissionStatus,
) {
    var status: MissionStatus = status
        private set   // <- can only change inside this class

    fun close() {
        check(status == MissionStatus.OPEN) { "Only an open mission can be closed" }
        status = MissionStatus.CLOSED
    }

    companion object {
        fun publish(title: String, /* ... */): Mission {
            require(title.isNotBlank()) { "Mission title must not be blank" }
            // ...
            return Mission(id = MissionId.generate(), /* ... */, status = MissionStatus.OPEN)
        }
    }
}
```

Three deliberate design choices, each enforcing an invariant:
- The **constructor is `private`**. The only ways to get a `Mission` are the `publish()` factory (creates a brand-new, valid, `OPEN` mission) and `reconstitute()` (used only by the persistence adapter, to rebuild a `Mission` that already passed validation once when it was first saved). You cannot accidentally construct a `Mission` in an inconsistent state from outside the class.
- **`status` has a private setter.** The only way to change it is to call a method on `Mission` itself, like `close()` - never `mission.status = MissionStatus.CLOSED` from calling code. This is what "aggregate root as sole entry point" means in practice.
- **`close()` checks the current state before allowing the transition.** `check(status == MissionStatus.OPEN)` throws `IllegalStateException` if you try to close an already-closed mission. This invariant - "a mission can only transition from OPEN to CLOSED once" - is enforced in exactly one place, inside the aggregate, instead of being (re-)implemented, and possibly forgotten, everywhere `close()` might be called from.

### 3.6 Domain events

A **domain event** is a statement of fact, named in the past tense, about something that happened in the domain - `MissionPublished`, not `PublishMission` (that's a command, a request for something to happen; the event is the record that it *did*).

```kotlin
// backend/sourcing/.../domain/event/MissionPublished.kt
data class MissionPublished(
    val missionId: MissionId,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val dailyRateCurrency: String,
    val startDate: LocalDate,
    override val metadata: EventMetadata = EventMetadata(),
) : DomainEvent
```

Events are how bounded contexts talk to each other without knowing about each other's internals - Section 5 covers the mechanics (Kafka) in depth. For now, the important domain-modeling point: an event carries only the *facts relevant to whoever might react to it*, not the whole aggregate. `MissionPublished` doesn't include the client's name, because nothing downstream (Matching) needs it to compute a score.

### 3.7 Domain services

Some logic doesn't naturally belong to any single entity or value object, because it needs to reason about *two* of them together. Forcing it into one or the other is awkward and arbitrary. That's what a **domain service** is for - a stateless piece of domain logic that takes the objects it needs as parameters.

`MatchingPolicy` is the clearest example in this codebase:

```kotlin
// backend/matching/.../domain/MatchingPolicy.kt
class MatchingPolicy {
    fun score(mission: MissionSnapshot, profile: ProfileSnapshot): MatchingScore {
        val weighted = SKILL_WEIGHT * skillOverlapRatio(mission, profile) +
                        RATE_WEIGHT * rateCompatibility(mission, profile)
        return MatchingScore(weighted.coerceIn(0.0, 1.0))
    }
    // ...
}
```

It doesn't belong on `MissionSnapshot` ("does a mission know how well it fits a random profile?" - no, that's backwards) or on `ProfileSnapshot` (same problem, mirrored). It's genuinely a relationship between the two, so it gets its own class. Notice it's **plain Kotlin with zero framework dependencies** - no `@Service`, no Spring import. It's pure business logic, which is exactly why it's trivial to unit test with no mocks at all (see Section 7).

### 3.8 Shared kernel

A **shared kernel** is a small piece of model that multiple bounded contexts explicitly agree to depend on together. It should stay tiny, because every context depending on it has to agree before it changes - the opposite of the usual DDD instinct to keep contexts independent.

`shared-kernel` in this repo holds exactly three things: `Money`, `SkillSet`, and the `DomainEvent`/`EventMetadata` envelope. Never a `Mission`, never a `Profile` - those are context-specific and deliberately duplicated in concept (each context that needs the idea of "a mission" defines its *own* minimal `MissionId`, see Section 5.3) rather than shared, because sharing them would recreate the "one big model" problem DDD is trying to avoid.

---

## 4. Hexagonal Architecture - a complete walkthrough

### 4.1 The problem it solves

If `Mission` (the domain aggregate) directly used JPA annotations, and the matching algorithm was triggered directly inside a `@RestController` method, two things would go wrong over time: (1) every framework upgrade risks touching business rules by accident, and (2) testing a business rule means spinning up a database and a web server, because the rule is welded to them.

**Hexagonal architecture** (Ports & Adapters, coined by Alistair Cockburn) solves this with one rule: *business logic must not know which technology is calling it, or which technology it's calling out to.*

### 4.2 The three layers

Every backend module in this repo (`sourcing`, `matching`, `freelancer-profile`) has exactly this shape:

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

**The dependency rule: arrows only point inward.** `infrastructure` depends on `application`, which depends on `domain`, and `domain` depends on nothing at all - not even Spring. You could delete every Spring Boot import from the project and `domain/Mission.kt` would still compile.

### 4.3 Ports: input and output

A **port** is an interface, owned by the `application` layer, that names a capability - completely independent of *how* that capability is fulfilled.

- **Input ports** live in `application/port/input/` and describe what the outside world can ask this module to do. `PublishMissionUseCase` is one:

  ```kotlin
  // backend/sourcing/.../application/port/input/PublishMissionUseCase.kt
  interface PublishMissionUseCase {
      fun publish(command: PublishMissionCommand): MissionId
  }
  ```

  (Named `port/input`, not `port/in` - `in` is a reserved keyword in Kotlin, used for generic variance and `for` loops. This is a small but real example of a language constraint shaping a naming convention.)

- **Output ports** live in `application/port/output/` and describe what this module *needs* from the outside world. `MissionRepository` is one:

  ```kotlin
  // backend/sourcing/.../application/port/output/MissionRepository.kt
  interface MissionRepository {
      fun save(mission: Mission): Mission
      fun findById(missionId: MissionId): Mission?
      fun findAll(): List<Mission>
  }
  ```

  Notice this interface says nothing about SQL, JPA, or Postgres. It's phrased entirely in domain terms: save a `Mission`, find a `Mission`. The application layer knows it needs *persistence* as a capability; it has no idea, and doesn't care, that the actual implementation happens to be a Postgres table.

### 4.4 Adapters: driving and driven

An **adapter** is a concrete implementation of a port using one specific technology. There are two flavors, distinguished by which direction the call comes from:

- A **driving adapter** *initiates* a call into the application layer. `MissionController` (a REST controller) is a driving adapter: an HTTP request comes in, and it calls `publishMissionUseCase.publish(...)`.
- A **driven adapter** is *called by* the application layer to fulfill an output port. `MissionRepositoryAdapter` (JPA-backed) is a driven adapter: the application layer calls `missionRepository.save(mission)`, without knowing that "save" means "run an SQL `INSERT` or `UPDATE`".

### 4.5 A complete walkthrough: publishing a mission

This is the most useful thing you can do to understand hexagonal architecture: follow one request through every layer, in the actual codebase, and see exactly which file does what.

**Step 1 - HTTP arrives at the driving adapter.**

```kotlin
// backend/sourcing/.../infrastructure/adapter/input/web/MissionController.kt
@RestController
@RequestMapping("/api/missions")
class MissionController(
    private val publishMissionUseCase: PublishMissionUseCase,   // <- depends on the PORT
    // ...
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun publish(@RequestBody request: PublishMissionRequest): MissionResponse {
        val command = PublishMissionCommand(
            title = request.title,
            clientName = request.clientName,
            requiredSkills = request.requiredSkills,
            dailyRateAmount = request.dailyRateAmount,
            startDate = request.startDate,
        )
        val missionId = publishMissionUseCase.publish(command)
        val mission = getMissionUseCase.getById(missionId) ?: error("...")
        return MissionResponse.from(mission)
    }
}
```

Notice `MissionController` depends on `PublishMissionUseCase` - the *interface*, the port - never on the class that implements it. It has no idea whether that implementation talks to Postgres or an in-memory map. It also translates: the wire-format `PublishMissionRequest` (a DTO shaped for JSON) becomes a `PublishMissionCommand` (a plain Kotlin data class the application layer understands). The controller's whole job is this translation plus HTTP mechanics (status codes) - zero business logic.

**Step 2 - the application service orchestrates the domain.**

```kotlin
// backend/sourcing/.../application/MissionApplicationService.kt
class MissionApplicationService(
    private val missionRepository: MissionRepository,           // output port
    private val missionEventPublisher: MissionEventPublisher,   // output port
) : PublishMissionUseCase, CloseMissionUseCase, GetMissionUseCase {   // implements input ports

    override fun publish(command: PublishMissionCommand): MissionId {
        val mission = Mission.publish(                    // 1. ask the domain to create itself
            title = command.title,
            clientName = command.clientName,
            requiredSkills = SkillSet.of(command.requiredSkills),
            dailyRate = Money(command.dailyRateAmount),
            startDate = command.startDate,
        )

        missionRepository.save(mission)                    // 2. persist, via the output port
        missionEventPublisher.publish(                      // 3. announce it happened
            MissionPublished(
                missionId = mission.id,
                requiredSkills = mission.requiredSkills.skills,
                dailyRateAmount = mission.dailyRate.amount,
                dailyRateCurrency = mission.dailyRate.currency,
                startDate = mission.startDate,
            ),
        )
        return mission.id
    }
}
```

This class is the heart of hexagonal architecture: it **orchestrates** - asks the domain to do the real work (`Mission.publish(...)`), then uses output ports to have side effects happen (`save`, `publish`) - but it contains no business *rules* itself. "A title cannot be blank" lives in `Mission.publish()`, not here. If that rule changes, this file doesn't.

**Step 3 - the domain enforces its own rules**, as covered already in Section 3.5's walkthrough of `Mission.publish()`.

**Step 4 - driven adapters do the actual I/O.**

```kotlin
// backend/sourcing/.../infrastructure/adapter/output/persistence/MissionRepositoryAdapter.kt
@Component
class MissionRepositoryAdapter(
    private val jpaRepository: MissionJpaRepository,   // Spring Data JPA
) : MissionRepository {                                 // implements the output port

    override fun save(mission: Mission): Mission {
        val saved = jpaRepository.save(MissionEntity.fromDomain(mission))
        return saved.toDomain()
    }
    // ...
}
```

Two things worth noticing: (1) this class is the *only* place `Mission` (domain) and `MissionEntity` (a separate class, annotated with `@Entity`, `@Table`, etc.) ever meet - the conversion happens right here, so JPA annotations never contaminate the domain class. (2) It's annotated `@Component`, the only place in this entire walkthrough where a Spring annotation appears on something touching business data - and it's on the *adapter*, exactly where the hexagonal rule says framework dependencies belong.

```kotlin
// backend/sourcing/.../infrastructure/adapter/output/messaging/KafkaMissionEventPublisher.kt
@Component
class KafkaMissionEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : MissionEventPublisher {
    override fun publish(event: DomainEvent) {
        kafkaTemplate.send(topicFor(event), keyFor(event), event)
    }
    // ...
}
```

Same pattern, different technology: this adapter turns "publish this domain event" (an abstract idea, from the output port's point of view) into "send this JSON payload to this Kafka topic" (a concrete mechanism).

**The payoff.** Because `MissionApplicationService` only knows about `MissionRepository` and `MissionEventPublisher` as *interfaces*, you can write a test for it (see Section 7.2) that hands it fake, in-memory implementations of both - no database, no Kafka broker, no Spring context - and still exercise the real orchestration logic. That's not a nice side effect of hexagonal architecture; it *is* the point.

### 4.6 Two driving adapters, one use case

One more thing worth seeing explicitly: `MatchingApplicationService` (in the `matching` module) is called by *three different driving adapters* - a Kafka consumer reacting to `MissionPublished`, another reacting to `ProfileUpdated`, and a REST controller answering `GET /api/matches`. All three call into the same application service, through its input ports. The business rule for "how do we score a match" doesn't fork depending on who's asking. That's the practical payoff of ports: swap the adapter, keep the rule.

---

## 5. Event-driven architecture with Kafka

### 5.1 Choreography vs orchestration

There are two ways to coordinate work across services. **Orchestration**: a central process calls each step explicitly, in order - "publish the mission, then tell Matching to score it, then tell ApplicationTracking to create a candidature." **Choreography**: each service reacts independently to events it cares about, and nobody is in charge of the overall sequence.

MissionMatch uses choreography, end to end. Sourcing publishes `MissionPublished` and has no idea Matching exists, let alone that it will react. This mirrors real organizations - the sourcing team doesn't manage the matching team's workflow - and it's also why the module boundaries hold: **the only legal way for one context to affect another is to publish an event it consumes.** No context imports another context's domain classes; grep the codebase and you won't find `import com.missionmatch.sourcing.domain.*` anywhere inside `matching`.

### 5.2 Anatomy of one event flow, step by step

Follow a mission from "published" to "matched":

**1. Sourcing constructs and publishes the event** (excerpt from Section 4.5's `MissionApplicationService.publish()`):

```kotlin
missionEventPublisher.publish(
    MissionPublished(missionId = mission.id, requiredSkills = mission.requiredSkills.skills, /* ... */),
)
```

**2. The driven adapter puts it on Kafka:**

```kotlin
// KafkaMissionEventPublisher.kt
override fun publish(event: DomainEvent) {
    val topic = topicFor(event)   // MissionPublished -> "mission-published"
    val key = keyFor(event)       // the mission's id, as a string
    kafkaTemplate.send(topic, key, event)
}
```

The **key** matters: Kafka guarantees ordering only *within a partition*, and messages with the same key always land on the same partition. Keying by `missionId` means every event about the same mission is processed in order, relative to *itself* - but, importantly, not necessarily in order relative to events on other topics (Section 6.2 is a real bug caused by exactly this).

**3. Matching's driving adapter (a Kafka consumer) receives it:**

```kotlin
// backend/matching/.../infrastructure/adapter/input/messaging/MissionPublishedConsumer.kt
@Component
class MissionPublishedConsumer(
    private val handleMissionPublishedUseCase: HandleMissionPublishedUseCase,
) {
    @KafkaListener(topics = [MISSION_PUBLISHED_TOPIC], groupId = "matching")
    fun onMissionPublished(event: MissionPublishedIntegrationEvent) {
        handleMissionPublishedUseCase.handle(
            MissionPublishedCommand(
                missionId = MissionId(event.missionId),
                requiredSkills = event.requiredSkills,
                dailyRateAmount = event.dailyRateAmount,
                dailyRateCurrency = event.dailyRateCurrency,
            ),
        )
    }
}
```

**4. Matching's application service reacts:** saves its own snapshot of the mission, then scores it against every known freelancer profile, publishing `MatchComputed` for anything above the eligibility threshold (see `MatchingApplicationService.handle(MissionPublishedCommand)` and `MatchingPolicy.score()` in Section 3.7).

### 5.3 The anti-corruption layer: why Matching doesn't just reuse Sourcing's event class

Look closely at step 3 above: the consumer deserializes into `MissionPublishedIntegrationEvent` - a class defined *inside the `matching` module*, not Sourcing's `MissionPublished`. This is deliberate and it's called an **anti-corruption layer**:

```kotlin
// backend/matching/.../infrastructure/adapter/input/messaging/MissionEventDtos.kt

// Anti-corruption layer: mirrors Sourcing's wire format without depending on its domain classes.
data class MissionPublishedIntegrationEvent(
    val missionId: UUID,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val dailyRateCurrency: String,
    val startDate: LocalDate,
)
```

If Matching deserialized straight into Sourcing's `MissionPublished` class, Matching's build would need to depend on Sourcing's module - reintroducing exactly the coupling bounded contexts are supposed to eliminate, and meaning any change to Sourcing's internal event shape could break Matching's compilation, even though the two teams (conceptually) never talk. Instead, Matching defines its own minimal DTO, shaped only for what *it* needs (notice: no `startDate` field is used by Matching's scoring, even though it's present - kept here for a future feature, harmless either way). The wire format is the *contract*; each side owns its own translation of that contract into code it controls.

The same pattern repeats for `MissionId`, `FreelancerId`: Matching defines its *own* `MissionId` and `FreelancerId` value objects (in `matching/domain/`), distinct from Sourcing's `MissionId` and FreelancerProfile's `FreelancerId`. They happen to all wrap a `UUID`, but they are different types the compiler will not let you mix up - you cannot accidentally pass a Sourcing `MissionId` where a Matching `MissionId` is expected. Each context's notion of "a mission's identity" is independently defined, on purpose.

### 5.4 Getting JSON across the anti-corruption boundary: type aliases, not class names

There's a subtlety in making the anti-corruption layer actually work over the wire. Spring Kafka's `JsonSerializer`/`JsonDeserializer` need to agree, somehow, on what Kotlin type a JSON payload should become. The naive approach - embedding the producer's fully-qualified Java class name in a Kafka message header - would defeat the whole point: the consumer's deserialization would depend on knowing the *producer's* class name, recreating the coupling we just eliminated.

The fix used throughout `application.yml` is a **type alias mapping**, agreed on both sides via a short string, not a class name:

```yaml
# Producer side (Sourcing, Matching, FreelancerProfile all publish through this one config,
# since they share one deployable process - see Section 8):
spring.json.type.mapping: >
  mission-published:com.missionmatch.sourcing.domain.event.MissionPublished,
  mission-closed:com.missionmatch.sourcing.domain.event.MissionClosed,
  match-computed:com.missionmatch.matching.domain.event.MatchComputed,
  profile-updated:com.missionmatch.freelancerprofile.domain.event.ProfileUpdated

# Consumer side (Matching, since it's the one consuming these three topics):
spring.json.type.mapping: >
  mission-published:com.missionmatch.matching.infrastructure.adapter.input.messaging.MissionPublishedIntegrationEvent,
  mission-closed:com.missionmatch.matching.infrastructure.adapter.input.messaging.MissionClosedIntegrationEvent,
  profile-updated:com.missionmatch.matching.infrastructure.adapter.input.messaging.ProfileUpdatedIntegrationEvent
```

Read this carefully: **the alias `mission-published` maps to a *different* Kotlin class on each side.** The producer maps it to *its own* domain event; the consumer maps the *same alias* to its own anti-corruption DTO. Kafka carries the alias `mission-published` in a message header (`__TypeId__`), and each side independently resolves that alias to whatever local type makes sense for it. Neither side needs to know the other's class even exists. This is the anti-corruption layer, made to actually work over a wire protocol, not just a diagram.

This one global property has exactly one limit: it can only map an alias to *one* class per side. That's fine as long as each topic has a single consumer context - it stops being fine the moment two contexts both need their own DTO for the same topic. Section 6.7 walks through exactly that case, and the per-context container factory it takes to get past it.

---

## 6. War stories: real distributed-systems bugs this project hit

This section is different from the rest of the guide: it's not a description of a pattern applied correctly from the start. These are bugs that were found by actually running the full system - real Kafka, real Postgres, real concurrent consumers, real Spring context startup - and fixed. They're included because reading about "eventual consistency" and "at-least-once delivery" in the abstract doesn't prepare you for what they actually look like when they bite. Each is documented in more detail in the project's git history and the [README](../../README.md#event-driven-communication).

### 6.1 Type headers, or the missing Rosetta Stone

**Symptom:** Matching's Kafka consumers threw `IllegalStateException: No type information in headers and no default type provided` for every message.

**Cause:** the first version of the producer config disabled type headers entirely (`spring.json.add.type.headers: false`), thinking "the consumer already knows its own DTO type, why send type info at all?" But Spring Kafka's `JsonDeserializer` needs *something* in the message to decide which class to deserialize into, and with headers off and no default type configured, it had nothing to go on.

**Fix:** the type-alias mapping described in Section 5.4 - headers stay on, but they carry a short, stable alias instead of a fully-qualified class name.

### 6.2 A value object that serialized as an object, not a value

**Symptom:** deserialization failed with `Cannot deserialize value of type java.util.UUID from Object value (token START_OBJECT)`.

**Cause:** `MissionId` is a `data class MissionId(val value: UUID)`. By default, Jackson serializes a data class as a JSON *object*: `{"value": "the-uuid"}`. But the anti-corruption DTO on the consuming side declares `missionId: UUID` - a plain scalar. Jackson choked trying to fit an object into a UUID field.

**Fix:** annotate the wrapped field with Jackson's `@JsonValue`:

```kotlin
data class MissionId(@get:JsonValue val value: UUID)
```

This tells Jackson "when serializing this type, use *this property's value* directly as the representation, not an object wrapping it." `MissionId` now serializes as a plain UUID string on the wire - matching what the anti-corruption DTOs on the other side actually expect.

### 6.3 The Hibernate session was already closed

**Symptom:** `LazyInitializationException: failed to lazily initialize a collection of role: MissionEntity.requiredSkills - no Session`, but only when hitting the real HTTP endpoint, never in the repository-level integration test.

**Cause:** `spring.jpa.open-in-view: false` is set deliberately (leaving it `true`, Spring Boot's default, is a well-known anti-pattern: it keeps a database connection open for the entire HTTP request, including view rendering, which doesn't scale). But `@ElementCollection` fields (like `requiredSkills`) default to `FetchType.LAZY`. With the session closed the moment the repository call returns, accessing `mission.requiredSkills` later, while building the JSON response, hit a collection that was never actually loaded, on a session that no longer existed.

The repository-level test didn't catch this because `@DataJpaTest` wraps each test in a transaction that stays open for the whole test method, so the lazy collection loaded without complaint - masking the exact failure that a real request, with its short-lived session, would hit.

**Fix:** mark the collection `FetchType.EAGER`. For a value this small and always needed alongside the mission itself, eager loading is the right call - lazy loading exists to defer *expensive, optional* data, and `requiredSkills` is neither.

### 6.4 Duplicate matches from two consumer threads racing

**Symptom:** the same `(missionId, freelancerId)` pair appeared twice in `match_results`, with the same score, computed milliseconds apart.

**Cause:** `mission-published` and `profile-updated` are two different Kafka topics, consumed by two independent listener threads. Publishing several missions and then immediately updating a profile that matched several of them let both consumer threads compute a match for the *same* mission-profile pair at nearly the same instant - a classic time-of-check-to-time-of-use (TOCTOU) race: both threads checked "does a match already exist for this pair?", both got "no," and both inserted.

**Fix:** an application-level check-then-insert can never fully close this race across threads - only the database can guarantee it. A unique constraint on `(mission_id, freelancer_id)` makes the database reject the second insert, and the repository adapter treats that specific failure as "someone else already recorded this," fetching and returning the winner's row instead of propagating an error:

```kotlin
override fun save(matchResult: MatchResult): MatchResult =
    try {
        jpaRepository.save(MatchResultEntity.fromDomain(matchResult)).toDomain()
    } catch (violation: DataIntegrityViolationException) {
        jpaRepository.findByMissionIdAndFreelancerId(matchResult.missionId.value, matchResult.freelancerId.value)
            ?.toDomain() ?: throw violation
    }
```

### 6.5 A mission closed before it existed, from Matching's point of view

**Symptom:** closing a mission in Sourcing had no effect on Matching's copy of it - it stayed "open" forever, even minutes later.

**Cause, first hypothesis (wrong):** "maybe the `MissionClosed` event gets reprocessed *after* `MissionPublished`, undoing the closure." Reasonable-sounding, and wrong - verified wrong by adding temporary logging and reading the actual event order.

**Cause, actually:** `mission-published` and `mission-closed` are, again, two different topics. Kafka gives ordering guarantees *within* a topic-partition, never *across* topics. Each consumer thread becomes "ready" (finishes its own partition assignment / rebalance) independently. In the observed run, the `mission-closed` consumer became ready and processed its event *before* the `mission-published` consumer had even started - so `MissionClosed` arrived at a service that had no snapshot yet to close, found nothing, and correctly did nothing (by the logic at the time). Then `MissionPublished` arrived and created a fresh snapshot, defaulting it to open - because, as far as that handler could tell, this mission had never been closed.

**Fix:** don't rely on arrival order at all. Record the fact "this mission is closed" independently of whether a snapshot exists yet, in its own tiny table:

```kotlin
override fun handle(command: MissionClosedCommand) {
    missionSnapshotRepository.markClosed(command.missionId)          // always, unconditionally
    val mission = missionSnapshotRepository.findById(command.missionId) ?: return
    missionSnapshotRepository.save(mission.copy(open = false))
}

override fun handle(command: MissionPublishedCommand) {
    val open = when {
        missionSnapshotRepository.isMarkedClosed(command.missionId) -> false
        else -> missionSnapshotRepository.findById(command.missionId)?.open ?: true
    }
    // ... build and save the snapshot with this `open` value
}
```

This is correct no matter which event arrives first, because neither handler assumes anything about ordering - each just checks the durable fact and acts accordingly. The lesson, stated generally: **when two events are causally related but travel on different topics, don't fix the symptom by tuning timing (a sleep, a delay); fix the model so correctness doesn't depend on arrival order at all.**

### 6.6 A validation error that came back as a 500

**Symptom:** sending an intentionally illegal candidature transition (e.g. `TO_APPLY` straight to `ACCEPTED`) returned `500 Internal Server Error` with no useful body, instead of a `400` explaining what was wrong.

**Cause:** `Candidature.moveTo()` throws `IllegalArgumentException` when the requested transition isn't in `ALLOWED_TRANSITIONS` - correct domain behavior. But no controller anywhere translated that exception into an HTTP status. Spring Boot's default behavior for any uncaught exception reaching a controller is a generic `500`, which is technically true (something did go wrong) but useless to a client: a `500` says "we broke," a `400` says "you sent something invalid," and the two demand completely different responses from whoever's calling the API. Sourcing's `close()` endpoint had the exact same silent gap - it just hadn't been exercised with a bad request yet.

**Fix:** one `@RestControllerAdvice` at the composition root (`bootstrap`, not any individual context) mapping `IllegalArgumentException`/`IllegalStateException` to `400` and `NoSuchElementException` to `404`:

```kotlin
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleInvalidRequest(exception: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(exception.message ?: "Invalid request"))

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(exception: NoSuchElementException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(exception.message ?: "Not found"))
}
```

This is a cross-cutting web concern, not a domain concern - it belongs exactly once, at the one place that assembles every context into HTTP endpoints. Fixing it here fixed both ApplicationTracking's new endpoint and Sourcing's pre-existing one, with zero changes to either context's own code.

### 6.7 One topic, two consumers, one type mapping - not enough

**Symptom:** as soon as `notification` also needed to consume `match-computed` (Section 3.3's context map shows both ApplicationTracking and Notification depending on it), it became clear the existing setup couldn't serve both - caught by reasoning about the config before ever running the app, not by a crash.

**Cause:** `spring.json.type.mapping` is one property, global to the consumer side of the shared Kafka config (Section 5.4). It maps the alias `match-computed` to exactly one class. But ApplicationTracking and Notification each need their *own* anti-corruption DTO for the same event (Section 5.3's rule - never share a DTO across contexts) - two different target classes for the same alias, which one global property literally cannot express.

**Fix:** Kafka doesn't require every consumer of a topic to deserialize it the same way - deserialization is a per-consumer-group client concern, not a topic-level property. So each context that needs its own DTO for `match-computed` gets its own `ConcurrentKafkaListenerContainerFactory`, built manually and bound to its own `JsonDeserializer`, referenced explicitly from its `@KafkaListener`:

```kotlin
@Bean
fun applicationTrackingKafkaListenerContainerFactory():
    ConcurrentKafkaListenerContainerFactory<String, MatchComputedIntegrationEvent> {
    val valueDeserializer = JsonDeserializer(MatchComputedIntegrationEvent::class.java, false).apply {
        addTrustedPackages("com.missionmatch.*")
    }
    val consumerFactory = DefaultKafkaConsumerFactory(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ),
        StringDeserializer(), valueDeserializer,
    )
    return ConcurrentKafkaListenerContainerFactory<String, MatchComputedIntegrationEvent>().apply {
        this.consumerFactory = consumerFactory
    }
}
```

```kotlin
@KafkaListener(topics = ["match-computed"], groupId = "application-tracking", containerFactory = "applicationTrackingKafkaListenerContainerFactory")
```

`notification` defines the mirror image, bound to its own `MatchComputedIntegrationEvent` class. `application.yml`'s global `spring.json.type.mapping` now deliberately omits `match-computed` entirely, with a comment explaining why - the two dedicated factories handle it instead.

### 6.8 Two classes, same name, one Spring context

**Symptom:** `bootRun` failed at startup with `ConflictingBeanDefinitionException: Annotation-specified bean name 'matchComputedConsumer' ... conflicts with existing bean definition`. Fixing it and restarting hit the *same* error again, this time for `matchComputedConsumerConfiguration`.

**Cause:** ApplicationTracking and Notification each declared their own `MatchComputedConsumer` (and, once Section 6.7's fix was written, their own `MatchComputedConsumerConfiguration`) - different packages, so Kotlin compiles both without complaint. But Spring's default component-scan bean naming uses the *simple*, unqualified class name, not the fully-qualified one. In a modular monolith, every module's `@Component`/`@Configuration` classes register into the *same* `ApplicationContext` - so two classes that happen to share a simple name collide in that one shared bean registry, even though nothing about the code itself is wrong.

**Fix:** explicit bean names wherever a simple name could plausibly collide across modules:

```kotlin
@Component("applicationTrackingMatchComputedConsumer")
class MatchComputedConsumer(...)
```

```kotlin
@Component("notificationMatchComputedConsumer")
class MatchComputedConsumer(...)
```

This is a real, structural consequence of choosing a modular monolith over separate microservices (Section 8.3): module boundaries stop the compiler from letting one context import another's classes, but they don't stop two contexts' simple class names from colliding in the one runtime registry they both share. Splitting either context into its own microservice later would make this class of bug structurally impossible - each process would have its own, separate `ApplicationContext`.

---

## 7. Test-Driven Development & Behavior-Driven Development

### 7.1 The testing pyramid, applied

Many fast, narrow tests at the bottom; fewer slow, broad tests at the top. This project's version of the pyramid maps directly onto the hexagonal layers:

| Layer | Tooling | What's real, what's faked |
|---|---|---|
| Domain | JUnit 5, AssertJ | Everything real - there's no infrastructure to fake |
| Application | JUnit 5, Mockito, AssertJ | Domain real, output ports mocked |
| Infrastructure adapters | JUnit 5, Testcontainers, AssertJ | Database/broker real (containerized), nothing mocked |

### 7.2 Domain layer: no mocks needed, and TDD is easiest here

**TDD (Test-Driven Development)** is a cycle: write a failing test for behavior that doesn't exist yet, write the minimum code to make it pass, then refactor with the safety net of that passing test. It's easiest to practice in the domain layer specifically because there's nothing to set up - no database, no HTTP, no mocks - just a pure function or object and an assertion.

```kotlin
// backend/sourcing/.../domain/MissionTest.kt
@Test
fun `closing an already closed mission is rejected`() {
    // Given
    val mission = Mission.publish(
        title = "Kotlin backend developer",
        clientName = "Acme Corp",
        requiredSkills = SkillSet.of("kotlin"),
        dailyRate = Money.of(600.0),
        startDate = LocalDate.now(),
    )
    mission.close()

    // When
    // Then
    assertThatThrownBy { mission.close() }.isInstanceOf(IllegalStateException::class.java)
}
```

This test would have been written *before* the `check(status == MissionStatus.OPEN)` line existed, in a genuine TDD flow: write this test (it fails, because nothing stops a second `close()` call yet), then add the `check(...)` to make it pass.

### 7.3 BDD: Given / When / Then, everywhere

**BDD (Behavior-Driven Development)** reframes tests around observable behavior - starting state, action, observable outcome - rather than implementation steps. Every single test in this codebase, at every layer, is structured with `// Given`, `// When`, `// Then` comments marking those three sections, whether or not a BDD framework like Cucumber is involved. The convention alone buys most of the clarity benefit: reading the test above, you don't need to know anything about `Mission` to understand *what's being verified* - a mission that's already closed, closed again, throws.

### 7.4 Application layer: mock the ports, never the domain

```kotlin
// backend/matching/.../application/MatchingApplicationServiceTest.kt
@Test
fun `a below-threshold score is neither persisted nor published`() {
    // Given
    val nonMatchingProfile = ProfileSnapshot(
        freelancerId = FreelancerId(UUID.randomUUID()),
        skills = SkillSet.of("php"),
        expectedDailyRate = Money.of(500.0),
    )
    whenever(profileSnapshotRepository.findAll()).thenReturn(listOf(nonMatchingProfile))
    val command = MissionPublishedCommand(
        missionId = MissionId(UUID.randomUUID()),
        requiredSkills = setOf("kotlin", "spring"),
        dailyRateAmount = BigDecimal.valueOf(600),
        dailyRateCurrency = "EUR",
    )

    // When
    service.handle(command)

    // Then
    verify(matchResultRepository, never()).save(any())
    verify(matchEventPublisher, never()).publish(any())
}
```

`profileSnapshotRepository` and `matchResultRepository` are Mockito mocks - genuinely fake, in-memory stand-ins for the *output ports* (interfaces). `MatchingPolicy`, the real domain service, is not mocked; it runs for real inside `service.handle(command)`, doing actual skill-overlap math. This is the practical benefit of Section 4's dependency rule made concrete: because `MatchingApplicationService` only depends on port *interfaces*, a test can substitute anything satisfying that interface, including a five-line Mockito stub, and still exercise real business logic with zero infrastructure.

### 7.5 Infrastructure adapters: Testcontainers, because mocking a database proves nothing

Unit tests structurally cannot catch a wrong SQL mapping, a Kafka serialization mismatch, or a missing index - because in a unit test, there's no real database or broker to be wrong *against*. **Testcontainers** solves this by running the real engine (Postgres, Kafka) in a throwaway Docker container for the duration of the test:

```kotlin
// backend/sourcing/.../infrastructure/adapter/output/persistence/MissionRepositoryAdapterTest.kt
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MissionRepositoryAdapterTest {

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

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            // ...
        }
    }
}
```

`@AutoConfigureTestDatabase(replace = Replace.NONE)` is important and easy to miss: by default, `@DataJpaTest` silently swaps in an embedded in-memory database, which would defeat the entire purpose of this test (an in-memory database doesn't have Postgres's actual SQL dialect quirks, constraint behavior, or connection semantics). This annotation says "no - use the real Postgres container I just started." None of the bugs in Section 6 (the JSON deserialization ones, the `LazyInitializationException`, the unique-constraint race) would have been caught by mocks; several were only caught by running the *entire real stack*, one level of realism beyond even what Testcontainers alone provides - Testcontainers is what makes a single adapter's integration test trustworthy, but only running the whole system catches bugs that live in the interaction *between* adapters.

---

## 8. A guided tour of the code structure

### 8.1 Top level

```
missionmatch/
├── backend/            # Kotlin/Spring Boot, one Gradle module per bounded context
├── frontend/            # Angular
├── docker-compose.yml    # Postgres + Kafka for local development
└── docs/                 # this guide
```

### 8.2 Inside a backend module

Every implemented context (`sourcing`, `matching`, `freelancer-profile`) follows the same internal shape, which is why Section 4.5's walkthrough of `sourcing` teaches you how to read `matching` and `freelancer-profile` too:

```
<context>/
├── domain/
│   ├── <Aggregate>.kt
│   ├── <ValueObject>.kt
│   └── event/
│       └── <DomainEvent>.kt
├── application/
│   ├── port/
│   │   ├── input/<UseCase>.kt
│   │   └── output/<Repository/Publisher>.kt
│   └── <Context>ApplicationService.kt
└── infrastructure/
    ├── <Context>Configuration.kt          # wires the application service as a Spring bean
    └── adapter/
        ├── input/
        │   ├── web/<Controller>.kt
        │   └── messaging/<Consumer>.kt
        └── output/
            ├── persistence/<Entity/Repository/Adapter>.kt
            └── messaging/<Publisher>.kt
```

Two module-level files are worth knowing about, because they don't fit inside a single context:

- `shared-kernel/` - see Section 3.8.
- `bootstrap/` - the single Spring Boot application that wires every context module together into one deployable process (see "Modular monolith" below). It holds `application.yml` (all configuration, including the Kafka type mappings from Section 5.4) and `DemoDataSeeder.kt` (a `@Profile("demo")`-gated component that populates realistic sample data by calling the *real* use cases - publishing missions, updating a profile - so the demo data goes through the exact same code path a real user's actions would, events and all).

### 8.3 Why one deployable process, not five microservices

MissionMatch ships as a single Spring Boot application - a **modular monolith**: each bounded context is its own Gradle module with its own hexagonal layers, but they all run in the same JVM process. This is a deliberate choice for a learning project: it gives full practice with DDD module boundaries and *real* Kafka messaging between contexts, without the operational cost of deploying, monitoring, and versioning five separate services.

The module boundaries are enforced strictly enough - no cross-module imports of `domain` or `application` classes, communication only through Kafka events (Section 5) or REST - that splitting any one module out into its own microservice later would be a deployment change, not a redesign. That's the real test of whether a "modular monolith" is genuinely modular: could you extract a module without touching its code, only its packaging?

---

## 9. The frontend, concept by concept

The frontend isn't just a UI on top of the backend concepts above - it applies its own set of small, deliberate patterns worth naming.

**Standalone components, no NgModules.** Every component (`MissionList`, `ChipInput`, `Sidebar`, ...) declares its own dependencies via an `imports: [...]` array in its `@Component` decorator, rather than being registered in a shared module. This keeps a component's dependency list local and explicit - reading the top of `mission-list.ts` tells you everything it needs, with nothing implied by which module happened to declare it.

**Signals for state, not RxJS subjects.** State like `MissionList.missions` is an Angular `signal<Mission[]>([])`, read reactively in the template (`missions()`), updated with `.set()`/`.update()`. This is the modern Angular idiom for local component state - simpler than an RxJS `BehaviorSubject` for state that doesn't need stream operators like `debounceTime` or `switchMap`.

**A reusable component built with `model()` for two-way binding.** `ChipInput` (the skill-tag editor used by both the mission form and the profile page) exposes its list of skills as `readonly skills = model.required<string[]>()`, letting a parent bind `[(skills)]="mySkillsSignal"` - Angular's signal-based equivalent of `[(ngModel)]`, generalized to any component's own state, not just form inputs.

**No backend for auth, so the frontend invents a stable local identity.** There's no login system yet. `shared/local-freelancer-id.ts` generates a UUID with `crypto.randomUUID()` on first visit and stores it in `localStorage`, so the same browser is recognized as "the same freelancer" across visits - used by the Profile page (to know whose profile to create/update) and the Matches page (to prefill "whose matches am I looking at"). This is a pragmatic stand-in, explicitly not a security mechanism, for a concept ( "who is the current user") that a real system would solve with actual authentication.

**A proxy, not CORS, for local development.** `frontend/proxy.conf.json` forwards any request to `/api/*` from the Angular dev server (port 4310) to the backend (port 8181). This makes the browser see everything as same-origin during development, sidestepping CORS entirely for the dev workflow (the backend also has an explicit CORS policy configured, for the case of hitting it directly without the proxy).

**Client-side rules that mirror, but never replace, server-side ones.** The `/candidatures` kanban board (`CandidatureBoard`) only shows the transition buttons `Candidature.moveTo()` would actually allow - a `TO_APPLY` card offers "Applied," never "Accepted." That list is duplicated on the frontend, in a plain `ALLOWED_TRANSITIONS` map next to the `Candidature` model, purely as a UI decision about which buttons to render:

```typescript
export const ALLOWED_TRANSITIONS: Record<CandidatureStatus, CandidatureStatus[]> = {
  TO_APPLY: ['APPLIED'],
  APPLIED: ['INTERVIEW', 'REJECTED'],
  INTERVIEW: ['ACCEPTED', 'REJECTED'],
  REJECTED: [],
  ACCEPTED: [],
};
```

This duplication is deliberate and safe *because* the backend never trusts it: every `PATCH /api/candidatures/{id}/status` re-validates the transition against the real aggregate rule in `Candidature.moveTo()`, and Section 6.6's exception handler turns a rejected one into a clean `400` the board can react to. If the two lists ever drifted apart, the UI would just offer a button that the server correctly refuses - a worse experience, never a correctness bug. The rule that actually matters lives in exactly one place: the aggregate.

---

## 10. Infrastructure as code: from six modules to a running system

Everything above this section runs the same way whether it's on a laptop or in AWS - that's what hexagonal architecture buys you. This section is about the part that *does* change: how the one deployable Spring Boot process, one Postgres instance, and one Kafka cluster described throughout this guide actually get provisioned on real infrastructure. As of this writing, `infra/terraform/` is written and passes `terraform validate`, but nothing in it has been `apply`'d yet - the AWS resources described here don't exist. That distinction matters and is kept honest deliberately, both in this guide and in the [README's roadmap](../../README.md#roadmap): reading Terraform that describes real infrastructure is a different, more useful exercise than reading a diagram, but it's still not the same as having watched it run.

### 10.1 Six modules, one concern each

`infra/terraform/modules/` mirrors the table in the README: `network` (VPC, subnets, NAT), `rds` (the one Postgres instance every context shares - Section 3.8's shared-nothing-except-infrastructure model), `msk` (managed Kafka, serverless so there's no broker capacity to plan), `ecs-service` (the Fargate service running the one deployable jar), `frontend-hosting` (S3 + CloudFront for the built Angular app), and `observability` (CloudWatch alarms). `environments/dev` and `environments/prod` compose the same six modules with different inputs - smaller instance sizes and a single NAT gateway for dev, Multi-AZ RDS and one NAT per AZ for prod - which is the same idea as this codebase's ports-and-adapters split, one level up: the modules are the stable interface, the environments are what varies.

### 10.2 A module dependency cycle, avoided by moving one resource up a level

`ecs-service` needs `rds`'s database endpoint and `msk`'s bootstrap brokers as inputs, to wire them into the container's environment variables. `rds` and `msk`, in turn, need to know the ECS tasks' security group id, so they can allow inbound traffic *only* from it. Each side needs something the other produces - a genuine cycle, and Terraform has no mechanism for a module to depend on another module that depends back on it.

The fix is the same kind of move used throughout this codebase whenever two things need to reference each other: introduce something upstream of both that neither depends on. Each environment's `main.tf` creates the ECS tasks' security group itself, directly, before calling any module:

```hcl
resource "aws_security_group" "ecs_tasks" {
  name_prefix = "${local.name_prefix}-ecs-"
  vpc_id      = module.network.vpc_id
}
```

`rds` and `msk` take its id as an input, to write their own ingress rules against it. `ecs-service` also takes it as an input - it doesn't create the security group, it only attaches one rule to it (`aws_vpc_security_group_ingress_rule`, "the ALB can reach the container port"), a rule that references a security group the module was handed, not one it owns. Nobody's output depends on the resource that creates them; the cycle is gone because the shared thing moved to where both sides could see it without needing each other.

### 10.3 Provisioning a repository before there's anything to put in it

`ecs-service` creates its own ECR repository (`aws_ecr_repository`), rather than assuming one exists. That reverses the usual order of operations: normally you'd build and push an image, *then* point infrastructure at it. Here, the infrastructure that will eventually pull the image is also what creates the shelf it sits on.

This works because an ECR repository URL is fully deterministic - `{account-id}.dkr.ecr.{region}.amazonaws.com/{repo-name}` - computable by anyone who knows the account, region, and name, without needing the repository to exist first or any Terraform output to have run. The practical consequence, spelled out in `infra/terraform/README.md`: the very first `terraform apply` for a new environment happens with a placeholder `container_image` (a public "hello world" image), because the real one can't be built and pushed to a repository that doesn't exist yet. Once it applies, `backend/Dockerfile` builds the real image, it's pushed to the now-existing repository, and a second `apply` swaps `container_image` for the real tag. Bootstrapping infrastructure sometimes means accepting a deliberately wrong first state to get to a place where the right one becomes possible.

### 10.4 One browser origin, two backends

CloudFront in `frontend-hosting` is configured with two origins, not one: the S3 bucket holding the built Angular app (the default behavior), and the ALB fronting the ECS service, matched only for the path pattern `/api/*`. This is the exact same reasoning already at work in local development, where `frontend/proxy.conf.json` forwards `/api/*` to the backend so the dev server and the API look same-origin to the browser (Section 9's proxy paragraph). CloudFront's `/api/*` behavior is that same idea, running in production instead of on a laptop: the Angular app's own client-side routes (`/missions`, `/candidatures`, ...) and the backend's REST resources never collide, because from the browser's point of view there is only ever one origin, and the split happens invisibly, one layer below it.

### 10.5 A password the application never sees as a literal

`rds` generates its master password with `random_password` and immediately writes it into a Secrets Manager secret - it's never a Terraform variable, never appears in a `.tfvars` file, never gets logged in a `terraform plan`. `ecs-service`'s task definition references that secret by ARN, using ECS's ability to pull a *specific key* out of a JSON secret at container-start time (`"${secret_arn}:username::"`, `"${secret_arn}:password::"`), so the running container gets real environment variables (`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) without that value ever having passed through the Terraform state file's plaintext form, or through anyone's hands, at all. Rotating the password becomes a Secrets Manager operation and an ECS service restart - not a code change, not a redeploy of anything Terraform manages.

### 10.6 Kafka's serverless offering makes one choice for you

MSK Serverless (the `msk` module) mandates SASL/IAM client authentication - there's no opting into a broker-managed username/password, unlike self-managed MSK. That's a real constraint the application has to satisfy, not just infrastructure: `backend/bootstrap/build.gradle.kts` pulls in `software.amazon.msk:aws-msk-iam-auth`, and a dedicated `aws` Spring profile (`application-aws.yml`) sets the Kafka client properties IAM auth needs (`sasl.mechanism: AWS_MSK_IAM`, a `IAMLoginModule`/`IAMClientCallbackHandler` pair) - inactive locally, where `docker-compose.yml`'s Kafka needs none of this, and active only once `SPRING_PROFILES_ACTIVE=aws` is set by `ecs-service`. The ECS task's IAM *role* (not a stored credential) is what authenticates to Kafka at runtime - `aws_iam_role_policy.task_kafka` grants that role exactly `kafka-cluster:Connect`/`ReadData`/`WriteData` on this cluster, nothing broader.

---

## 11. Glossary

- **Aggregate** - a cluster of domain objects treated as one unit for data changes, with one **aggregate root** as the only entry point external code may call. Guarantees invariants are never left inconsistent. *Example: `Mission`.*
- **Anti-corruption layer** - a translation boundary that stops one bounded context's internal model from leaking into another's. *Example: `MissionPublishedIntegrationEvent` in `matching`, a local copy of the wire format that doesn't depend on Sourcing's `MissionPublished` class.*
- **Application service** - orchestrates domain objects and output ports to fulfill a use case; contains no business rules of its own. *Example: `MissionApplicationService`.*
- **BDD (Behavior-Driven Development)** - describing and testing behavior from an observable, business-readable point of view (Given/When/Then) rather than implementation steps.
- **Bounded context** - a boundary within which a specific domain model and vocabulary is consistent and unambiguous; the same word can mean something different in another context. *Example: "Profile" means something different in `FreelancerProfile` (a full CV) vs `Matching` (a `ProfileSnapshot` with just skills and rate).*
- **Choreography** (vs. orchestration) - an integration style where each service reacts independently to events, with no central coordinator deciding the sequence.
- **Domain event** - a fact that happened in the domain, named in the past tense, that other parts of the system may react to. *Example: `MissionPublished`.*
- **Domain service** - domain logic that doesn't naturally belong to one entity or value object, because it needs two of them at once. *Example: `MatchingPolicy`.*
- **Driven adapter** - an adapter the application layer calls out to, to fulfill an output port. *Example: `MissionRepositoryAdapter` (JPA), `KafkaMissionEventPublisher`.*
- **Driving adapter** - an adapter that initiates a call into the application layer. *Example: `MissionController` (REST), `MissionPublishedConsumer` (Kafka).*
- **Entity** - an object defined by its identity (an ID), not its current attributes, whose state can change over time. *Example: `Mission`.*
- **Eventual consistency** - the guarantee that, absent new updates, all parts of a distributed system will *eventually* reflect the same state, but not necessarily at the same instant. *Example: a freelancer can see a mission in Sourcing a few hundred milliseconds before Matching has scored it.*
- **Hexagonal architecture** (Ports & Adapters) - an architecture where business logic (domain + application) depends on nothing external; all technology lives in adapters implementing or calling interfaces (ports) the business logic defines.
- **IAM role** - an AWS identity assumed by a resource (here, an ECS task) rather than a stored credential, granted only the specific permissions it needs. *Example: the ECS task role's `kafka-cluster:Connect` permission, scoped to exactly one MSK cluster, is what authenticates to Kafka instead of a username/password.*
- **Idempotency** - the property that processing the same operation (or event) more than once produces the same result as processing it once, with no extra side effects. *Example: the unique constraint on `match_results` making a duplicate insert a safe no-op instead of a duplicate row.*
- **Infrastructure as Code (IaC)** - describing infrastructure (servers, networks, databases) as versioned, reviewable configuration rather than manual console clicks, so provisioning it is repeatable and auditable the same way code is. *Example: `infra/terraform/`.*
- **Modular monolith** - a single deployable application internally split into strictly-bounded modules (here, one per bounded context), communicating only through the same kinds of boundaries (events, ports) a true microservices split would use.
- **Port** - an interface, owned by the application layer, naming a capability needed (output port) or offered (input port), independent of any specific technology. *Example: `MissionRepository` (output), `PublishMissionUseCase` (input).*
- **Shared kernel** - a small, explicitly-agreed-upon piece of model shared between bounded contexts, kept minimal because every dependent context must agree before it changes. *Example: `Money`, `SkillSet` in `shared-kernel`.*
- **TDD (Test-Driven Development)** - writing a failing test before the code that makes it pass, then refactoring, in short repeated cycles.
- **Testcontainers** - a library that runs real dependencies (databases, brokers) as throwaway Docker containers during a test run, so integration tests exercise the real engine instead of a mock.
- **Ubiquitous language** - the vocabulary shared between developers and domain experts within a bounded context, used literally in code - class names, method names - not just in comments or documentation.
- **Value object** - an object defined entirely by its attributes, immutable, with no identity; two value objects holding the same data are interchangeable. *Example: `Money`, `SkillSet`.*

---

*[Lire en français](../fr/ARCHITECTURE.md) · [Back to the README](../../README.md)*
