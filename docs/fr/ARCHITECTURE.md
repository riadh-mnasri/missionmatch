# MissionMatch - Guide complet de l'architecture et des concepts

*[Read in English](../en/ARCHITECTURE.md)*

Ce document est le compagnon pédagogique approfondi du [README](../../README.md) du projet. Le README est une référence : il dit ce qui existe et comment le faire tourner. Ce document est un *guide* : il enseigne, un par un, chaque concept que le code démontre, toujours ancré dans du vrai code de ce dépôt - jamais des exemples inventés.

Si vous connaissez déjà parfaitement le DDD, l'architecture hexagonale et les systèmes événementiels, ce document ne vous est pas nécessaire - le README suffit. Si l'un de ces sujets vous est nouveau, ou si vous voulez voir comment les définitions de manuel se traduisent en Kotlin et en TypeScript qui tournent réellement, continuez. Chaque section suit le même schéma : **ce qu'est le concept**, **pourquoi il existe (quel problème il résout)**, et **où il vit dans ce code**, avec de vrais extraits.

## Table des matières

1. [Comment lire ce projet](#1-comment-lire-ce-projet)
2. [Le problème métier, en termes simples](#2-le-problème-métier-en-termes-simples)
3. [Domain-Driven Design](#3-domain-driven-design)
4. [Architecture hexagonale - une visite guidée complète](#4-architecture-hexagonale--une-visite-guidée-complète)
5. [Architecture événementielle avec Kafka](#5-architecture-événementielle-avec-kafka)
6. [Récits de guerre : de vrais bugs distribués rencontrés sur ce projet](#6-récits-de-guerre--de-vrais-bugs-distribués-rencontrés-sur-ce-projet)
7. [Test-Driven Development & Behavior-Driven Development](#7-test-driven-development--behavior-driven-development)
8. [Visite guidée de la structure du code](#8-visite-guidée-de-la-structure-du-code)
9. [Le frontend, concept par concept](#9-le-frontend-concept-par-concept)
10. [Glossaire](#10-glossaire)

---

## 1. Comment lire ce projet

MissionMatch est une petite application de mise en relation missions freelance / freelances, mais elle est *délibérément* construite comme le serait un système bien plus gros et bien plus pérenne, afin que chaque pratique professionnelle qu'elle démontre puisse s'observer isolément, dans du vrai code qui tourne, plutôt que d'être décrite dans l'abstrait.

Conséquence pratique : ne jugez pas la quantité de cérémonie (une interface pour tout, un module par contexte borné, des DTO anti-corruption qui semblent redondants) à l'aune de la taille de la fonctionnalité qu'elle supporte. Une fonctionnalité "publier une mission", qui pourrait être un script de 20 lignes ailleurs, prend ici une douzaine de fichiers **exprès** - chaque fichier est un exemple travaillé d'une idée architecturale.

La meilleure façon d'utiliser ce guide est de l'avoir ouvert à côté de l'arborescence réelle du code. Chaque extrait ci-dessous est copié-collé depuis le dépôt, jamais simplifié ni inventé, afin que vous puissiez ouvrir le vrai fichier et le voir dans son contexte complet.

---

## 2. Le problème métier, en termes simples

Un freelance entend parler d'opportunités de missions par de nombreux canaux. Pour chacune, il doit vérifier manuellement : *ai-je les bonnes compétences ? Le TJM me convient-il ? Suis-je disponible ?* Puis, si ça semble prometteur, il postule et suit cette candidature à travers les entretiens jusqu'à ce qu'elle devienne un contrat - ou pas.

MissionMatch automatise l'étape du milieu - le matching - et donne de la visibilité sur le pipeline :

```
une mission est publiée
        │
        ▼
elle est scorée par rapport aux profils freelances
        │
        ▼
les bons matches deviennent des suggestions de candidature
        │
        ▼
le freelance les fait avancer dans un pipeline
        │
        ▼
tout le monde est notifié en chemin
```

Chaque flèche de ce diagramme est, ce n'est pas un hasard, une **frontière de contexte borné** dans le code. C'est le premier concept qu'il vaut la peine de bien comprendre.

---

## 3. Domain-Driven Design

### 3.1 Le problème que le DDD résout

Imaginez construire MissionMatch comme un seul gros modèle : une seule classe `Mission`, une seule classe `User`, une grosse couche de services. Au début, ça fonctionne. Mais "mission" signifie quelque chose de subtilement différent selon qui en parle : pour la personne qui source des missions, une mission est un cycle de vie (brouillon → ouverte → fermée). Pour le moteur de matching, une mission n'est qu'un ensemble de compétences requises et un tarif, utilisés pour le scoring. Pour une future fonctionnalité de facturation, une mission serait une ligne sur une facture. Forcez tout ça dans une seule classe `Mission` et vous obtenez un objet boursouflé, avec des champs qui n'ont de sens que pour certains de ses appelants, et des règles métier qui se contredisent selon la dernière fonctionnalité qui a touché la classe.

**La réponse du Domain-Driven Design :** ne construisez pas un seul modèle. Découpez le domaine en **contextes bornés** (*bounded contexts*), chacun avec son propre modèle, petit et cohérent en interne, et laissez chaque contexte définir "Mission" (ou tout autre concept) exactement comme *lui* a besoin de le voir, ni plus.

### 3.2 Langage ubiquitaire

À l'intérieur d'un contexte borné, le code doit se lire comme un expert du métier qui parlerait, pas comme des lignes de base de données. Dans ce dépôt, cela veut dire : pas de `MissionDTO`, pas de `MissionEntity` qui fuite dans la couche domaine, pas de `getMissionData()`. À la place :

```kotlin
// backend/sourcing/.../domain/Mission.kt
fun isEligibleForMatching(): Boolean = status == MissionStatus.OPEN

fun close() {
    check(status == MissionStatus.OPEN) { "Only an open mission can be closed" }
    status = MissionStatus.CLOSED
}
```

`isEligibleForMatching()` et `close()` sont des mots qu'un product manager emploierait. C'est le langage ubiquitaire à l'œuvre : le vocabulaire est partagé entre le code et les personnes qui comprennent le métier, et c'est *littéralement* le nom des méthodes, pas un commentaire qui traduit du jargon technique en termes métier.

### 3.3 Les contextes bornés de MissionMatch

MissionMatch compte cinq contextes bornés :

| Contexte | Racine d'agrégat | Possède | Publie | Écoute |
|---|---|---|---|---|
| **Sourcing** | `Mission` | Cycle de vie de la mission : créée, ouverte, fermée | `MissionPublished`, `MissionClosed` | - |
| **FreelancerProfile** | `Profile` | Compétences, TJM attendu | `ProfileUpdated` | - |
| **Matching** | `MatchResult` | Algorithme de scoring, historique des matches | `MatchComputed` | `MissionPublished`, `MissionClosed`, `ProfileUpdated` |
| **ApplicationTracking** | `Candidature` | Statut du pipeline (kanban) | `CandidatureStatusChanged` | `MatchComputed` |
| **Notification** | *(pas d'agrégat)* | Envoi d'e-mails/Slack | - | `MatchComputed`, `CandidatureStatusChanged` |

Pourquoi exactement ces frontières, et pas, disons, un seul énorme contexte "Missions" ?

- **Sourcing** et **FreelancerProfile** ne savent rien l'un de l'autre. Une mission est une mission parfaitement valide, qu'un moteur de matching existe ou non. Les garder séparés permet de changer la façon dont les missions sont sourcées sans jamais toucher au code des profils, et inversement.
- **Matching** est le *seul* contexte qui a besoin de raisonner sur une mission et un profil en même temps. Ce raisonnement - l'algorithme de scoring - est mis en quarantaine ici. S'il avait fui vers Sourcing (par exemple `Mission.computeMatchScore(profile)`), Sourcing dépendrait soudain des concepts de FreelancerProfile, sans aucune raison intrinsèque au fait de sourcer une mission.
- **ApplicationTracking** ne recalcule rien. Il réagit à un match en proposant au freelance quelque chose sur quoi agir. Son statut de pipeline (`TO_APPLY → APPLIED → INTERVIEW → REJECTED/ACCEPTED`) est *son propre concept* - Matching n'a aucun avis sur les pipelines de candidature, et ne devrait pas en avoir.
- **Notification** n'a aucun agrégat du tout, car il ne protège aucun état qui mériterait ses propres règles de cohérence. C'est un pur exécuteur d'effets de bord : un event arrive, un e-mail part. Tous les contextes n'ont pas besoin de la panoplie tactique complète du DDD (agrégats, invariants) - savoir *quand ne pas* utiliser un patron est aussi important que savoir l'utiliser.

### 3.4 La carte des contextes (*context map*)

```
 Sourcing ────MissionPublished, MissionClosed────┐
                                                   ▼
FreelancerProfile ──ProfileUpdated──▶  Matching  ──MatchComputed──▶ ApplicationTracking
                                                   │                       │
                                                   └───────────┬───────────┘
                                                                ▼
                                                          Notification
```

Les flèches sont des **events Kafka**, jamais des appels de méthode. Le code d'aucun contexte n'importe les classes du domaine d'un autre contexte. La section 5 explique précisément comment cette règle est *appliquée*, pas seulement suivie par convention.

### 3.5 Agrégats, Entités et Objets-valeur

Ces trois briques apparaissent constamment dans la couche domaine, et il vaut la peine d'être précis sur ce qu'est chacune, car elles résolvent des problèmes différents.

**Entité** - un objet défini par son *identité*, pas par ses valeurs d'attributs actuelles. Deux entités aux attributs identiques mais aux identifiants différents sont des choses différentes. `Mission` est une entité : deux missions pourraient avoir exactement le même titre, le même client, les mêmes compétences et le même tarif, et rester deux missions différentes, distinguées uniquement par leur `MissionId`.

**Objet-valeur** (*value object*) - un objet entièrement défini par ses attributs, sans identité propre. Deux objets-valeur portant les mêmes données *sont* la même valeur, interchangeables. `Money` et `SkillSet` (dans `shared-kernel`) sont des objets-valeur :

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

Deux choses à remarquer ici, vraies pour *chaque* objet-valeur de ce code :
1. C'est une `data class` Kotlin, qui donne gratuitement l'égalité structurelle (`Money(100, "EUR") == Money(100, "EUR")` vaut `true` même s'il s'agit de deux instances différentes) - exactement la sémantique "mêmes données = même valeur" dont un objet-valeur a besoin.
2. Son bloc `init` rend **impossible de construire un `Money` invalide**. Vous ne pouvez avoir un montant négatif ou une devise malformée nulle part dans le système, car aucun chemin de code ne produit une instance de `Money` sans passer par cette vérification. C'est une idée centrale du DDD : pousser la validation dans le type lui-même, pour qu'un "état invalide" ne soit pas juste découragé, mais *impossible à représenter*.

**Agrégat** - un ensemble d'entités et d'objets-valeur traité comme une seule unité pour les changements de données, avec une seule **racine d'agrégat** comme unique point d'entrée. La règle : rien à l'extérieur de l'agrégat n'a le droit d'aller modifier ses parties internes directement - chaque changement passe par la racine, qui peut ainsi faire respecter les invariants qui comptent.

`Mission` est une racine d'agrégat (ici l'agrégat entier est petit - juste la racine plus des objets-valeur, pas d'entités enfants, ce qui est courant et parfaitement normal). Regardez comment elle protège sa propre cohérence :

```kotlin
class Mission private constructor(
    val id: MissionId,
    val title: String,
    // ...
    status: MissionStatus,
) {
    var status: MissionStatus = status
        private set   // <- ne peut changer que depuis l'intérieur de cette classe

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

Trois choix de conception délibérés, chacun imposant un invariant :
- Le **constructeur est `private`**. Les seuls moyens d'obtenir une `Mission` sont la fabrique `publish()` (crée une mission toute neuve, valide, `OPEN`) et `reconstitute()` (utilisée uniquement par l'adapter de persistance, pour reconstruire une `Mission` déjà validée une première fois lors de sa sauvegarde initiale). Vous ne pouvez pas construire accidentellement une `Mission` dans un état incohérent depuis l'extérieur de la classe.
- **`status` a un setter privé.** La seule façon de le changer est d'appeler une méthode de `Mission` elle-même, comme `close()` - jamais `mission.status = MissionStatus.CLOSED` depuis le code appelant. C'est ce que "racine d'agrégat comme unique point d'entrée" veut dire en pratique.
- **`close()` vérifie l'état courant avant d'autoriser la transition.** `check(status == MissionStatus.OPEN)` lève une `IllegalStateException` si vous essayez de fermer une mission déjà fermée. Cet invariant - "une mission ne peut passer de OPEN à CLOSED qu'une seule fois" - est appliqué à exactement un seul endroit, à l'intérieur de l'agrégat, au lieu d'être (ré)implémenté, et potentiellement oublié, partout où `close()` pourrait être appelée.

### 3.6 Les events de domaine

Un **event de domaine** (*domain event*) est un constat de fait, nommé au passé, à propos de quelque chose qui s'est produit dans le domaine - `MissionPublished`, pas `PublishMission` (ça, c'est une commande, une demande pour que quelque chose se produise ; l'event est la trace que ça s'est *effectivement* produit).

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

Les events sont la façon dont les contextes bornés se parlent sans connaître les détails internes les uns des autres - la section 5 couvre la mécanique (Kafka) en profondeur. Pour l'instant, le point de modélisation important : un event ne porte que les *faits pertinents pour qui pourrait y réagir*, pas l'agrégat entier. `MissionPublished` n'inclut pas le nom du client, car rien en aval (Matching) n'en a besoin pour calculer un score.

### 3.7 Les services de domaine

Certaines logiques n'appartiennent naturellement à aucune entité ni objet-valeur unique, parce qu'elles doivent raisonner sur *deux* d'entre eux ensemble. Les forcer dans l'un ou l'autre serait maladroit et arbitraire. C'est à ça que sert un **service de domaine** : un bout de logique métier sans état, qui prend en paramètres les objets dont il a besoin.

`MatchingPolicy` en est l'exemple le plus clair dans ce code :

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

Ça n'appartient pas à `MissionSnapshot` ("une mission sait-elle à quel point elle correspond à un profil quelconque ?" - non, c'est à l'envers) ni à `ProfileSnapshot` (même problème, en miroir). C'est véritablement une relation entre les deux, donc ça mérite sa propre classe. Remarquez qu'elle est **du Kotlin pur, sans aucune dépendance à un framework** - pas de `@Service`, pas d'import Spring. C'est de la pure logique métier, et c'est exactement pour ça qu'elle est triviale à tester unitairement, sans aucun mock (voir section 7).

### 3.8 Le noyau partagé (*shared kernel*)

Un **noyau partagé** est un petit morceau de modèle que plusieurs contextes bornés acceptent explicitement de partager. Il doit rester minuscule, car chaque contexte qui en dépend doit donner son accord avant qu'il change - l'inverse de l'instinct habituel du DDD, qui est de garder les contextes indépendants.

`shared-kernel`, dans ce dépôt, contient exactement trois choses : `Money`, `SkillSet`, et l'enveloppe `DomainEvent`/`EventMetadata`. Jamais une `Mission`, jamais un `Profile` - ceux-là sont spécifiques à leur contexte et délibérément dupliqués en tant que concept (chaque contexte qui a besoin de l'idée "une mission" définit son *propre* `MissionId` minimal, voir section 5.3) plutôt que partagés, car les partager recréerait le problème du "gros modèle unique" que le DDD cherche justement à éviter.

---

## 4. Architecture hexagonale - une visite guidée complète

### 4.1 Le problème qu'elle résout

Si `Mission` (l'agrégat de domaine) utilisait directement des annotations JPA, et que l'algorithme de matching était déclenché directement à l'intérieur d'une méthode `@RestController`, deux choses se dégraderaient avec le temps : (1) chaque montée de version d'un framework risque de toucher des règles métier par accident, et (2) tester une règle métier implique de démarrer une base de données et un serveur web, parce que la règle y est soudée.

L'**architecture hexagonale** (*Ports & Adapters*, formulée par Alistair Cockburn) résout ça avec une seule règle : *la logique métier ne doit pas savoir quelle technologie l'appelle, ni quelle technologie elle appelle en retour.*

### 4.2 Les trois couches

Chaque module backend de ce dépôt (`sourcing`, `matching`, `freelancer-profile`) a exactement cette forme :

```
                 ┌─────────────────────────────────────┐
                 │           infrastructure              │
                 │  (adapters : web, Kafka, JPA, email)  │
                 │   ┌───────────────────────────────┐   │
                 │   │          application           │   │
                 │   │  (use cases, ports in/out)     │   │
                 │   │   ┌─────────────────────────┐  │   │
                 │   │   │        domain            │  │   │
                 │   │   │ (agrégats, VO, règles)    │  │   │
                 │   │   └─────────────────────────┘  │   │
                 │   └───────────────────────────────┘   │
                 └─────────────────────────────────────┘
```

**La règle de dépendance : les flèches ne pointent que vers l'intérieur.** `infrastructure` dépend de `application`, qui dépend de `domain`, et `domain` ne dépend de rien du tout - pas même de Spring. Vous pourriez supprimer tous les imports Spring Boot du projet, et `domain/Mission.kt` compilerait toujours.

### 4.3 Les ports : entrée et sortie

Un **port** est une interface, possédée par la couche `application`, qui nomme une capacité - complètement indépendante de *comment* cette capacité est réalisée.

- Les **ports d'entrée** (*input ports*) vivent dans `application/port/input/` et décrivent ce que le monde extérieur peut demander à ce module de faire. `PublishMissionUseCase` en est un :

  ```kotlin
  // backend/sourcing/.../application/port/input/PublishMissionUseCase.kt
  interface PublishMissionUseCase {
      fun publish(command: PublishMissionCommand): MissionId
  }
  ```

  (Nommé `port/input`, pas `port/in` - `in` est un mot-clé réservé en Kotlin, utilisé pour la variance générique et les boucles `for`. C'est un petit exemple bien réel d'une contrainte de langage qui façonne une convention de nommage.)

- Les **ports de sortie** (*output ports*) vivent dans `application/port/output/` et décrivent ce dont ce module a *besoin* du monde extérieur. `MissionRepository` en est un :

  ```kotlin
  // backend/sourcing/.../application/port/output/MissionRepository.kt
  interface MissionRepository {
      fun save(mission: Mission): Mission
      fun findById(missionId: MissionId): Mission?
      fun findAll(): List<Mission>
  }
  ```

  Remarquez que cette interface ne dit rien sur SQL, JPA ou Postgres. Elle est formulée entièrement en termes de domaine : sauvegarder une `Mission`, trouver une `Mission`. La couche application sait qu'elle a besoin de la *persistance* comme capacité ; elle n'a aucune idée, et ne se préoccupe pas, du fait que l'implémentation réelle soit une table Postgres.

### 4.4 Les adapters : conducteurs et conduits

Un **adapter** est une implémentation concrète d'un port utilisant une technologie précise. Il en existe deux saveurs, distinguées par la direction d'où vient l'appel :

- Un **adapter conducteur** (*driving adapter*) *initie* un appel vers la couche application. `MissionController` (un contrôleur REST) en est un : une requête HTTP arrive, et il appelle `publishMissionUseCase.publish(...)`.
- Un **adapter conduit** (*driven adapter*) est *appelé par* la couche application pour réaliser un port de sortie. `MissionRepositoryAdapter` (adossé à JPA) en est un : la couche application appelle `missionRepository.save(mission)`, sans savoir que "save" signifie "exécuter un `INSERT` ou `UPDATE` SQL".

### 4.5 Visite guidée complète : publier une mission

C'est la chose la plus utile que vous puissiez faire pour comprendre l'architecture hexagonale : suivre une requête à travers toutes les couches, dans le vrai code, et voir exactement quel fichier fait quoi.

**Étape 1 - la requête HTTP arrive sur l'adapter conducteur.**

```kotlin
// backend/sourcing/.../infrastructure/adapter/input/web/MissionController.kt
@RestController
@RequestMapping("/api/missions")
class MissionController(
    private val publishMissionUseCase: PublishMissionUseCase,   // <- dépend du PORT
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

Remarquez que `MissionController` dépend de `PublishMissionUseCase` - l'*interface*, le port - jamais de la classe qui l'implémente. Il n'a aucune idée si cette implémentation parle à Postgres ou à une map en mémoire. Il traduit aussi : le format d'échange `PublishMissionRequest` (un DTO façonné pour du JSON) devient un `PublishMissionCommand` (une simple data class Kotlin que la couche application comprend). Tout le travail du contrôleur, c'est cette traduction plus la mécanique HTTP (les codes de statut) - zéro logique métier.

**Étape 2 - le service applicatif orchestre le domaine.**

```kotlin
// backend/sourcing/.../application/MissionApplicationService.kt
class MissionApplicationService(
    private val missionRepository: MissionRepository,           // port de sortie
    private val missionEventPublisher: MissionEventPublisher,   // port de sortie
) : PublishMissionUseCase, CloseMissionUseCase, GetMissionUseCase {   // implémente les ports d'entrée

    override fun publish(command: PublishMissionCommand): MissionId {
        val mission = Mission.publish(                    // 1. demander au domaine de se créer
            title = command.title,
            clientName = command.clientName,
            requiredSkills = SkillSet.of(command.requiredSkills),
            dailyRate = Money(command.dailyRateAmount),
            startDate = command.startDate,
        )

        missionRepository.save(mission)                    // 2. persister, via le port de sortie
        missionEventPublisher.publish(                      // 3. annoncer que c'est arrivé
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

Cette classe est le cœur de l'architecture hexagonale : elle **orchestre** - demande au domaine de faire le vrai travail (`Mission.publish(...)`), puis utilise les ports de sortie pour déclencher des effets de bord (`save`, `publish`) - mais ne contient aucune règle métier elle-même. "Un titre ne peut pas être vide" vit dans `Mission.publish()`, pas ici. Si cette règle change, ce fichier ne change pas.

**Étape 3 - le domaine fait respecter ses propres règles**, comme déjà vu dans la visite de `Mission.publish()` en section 3.5.

**Étape 4 - les adapters conduits font les vraies entrées-sorties.**

```kotlin
// backend/sourcing/.../infrastructure/adapter/output/persistence/MissionRepositoryAdapter.kt
@Component
class MissionRepositoryAdapter(
    private val jpaRepository: MissionJpaRepository,   // Spring Data JPA
) : MissionRepository {                                 // implémente le port de sortie

    override fun save(mission: Mission): Mission {
        val saved = jpaRepository.save(MissionEntity.fromDomain(mission))
        return saved.toDomain()
    }
    // ...
}
```

Deux choses à remarquer : (1) cette classe est le *seul* endroit où `Mission` (domaine) et `MissionEntity` (une classe séparée, annotée `@Entity`, `@Table`, etc.) se rencontrent - la conversion se fait ici, et nulle part ailleurs, si bien que les annotations JPA ne contaminent jamais la classe de domaine. (2) Elle est annotée `@Component`, le seul endroit de toute cette visite guidée où une annotation Spring apparaît sur quelque chose qui touche aux données métier - et c'est sur l'*adapter*, exactement là où la règle hexagonale dit que les dépendances aux frameworks doivent vivre.

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

Même patron, technologie différente : cet adapter transforme "publie cet event de domaine" (une idée abstraite, du point de vue du port de sortie) en "envoie ce JSON sur ce topic Kafka" (un mécanisme concret).

**Le bénéfice final.** Parce que `MissionApplicationService` ne connaît `MissionRepository` et `MissionEventPublisher` que comme des *interfaces*, vous pouvez écrire un test pour lui (voir section 7.2) qui lui fournit de fausses implémentations, en mémoire, des deux - pas de base de données, pas de broker Kafka, pas de contexte Spring - et malgré tout exercer la vraie logique d'orchestration. Ce n'est pas un joli effet de bord de l'architecture hexagonale ; c'est *exactement* son but.

### 4.6 Deux adapters conducteurs, un seul cas d'usage

Une dernière chose à voir explicitement : `MatchingApplicationService` (dans le module `matching`) est appelé par *trois adapters conducteurs différents* - un consumer Kafka qui réagit à `MissionPublished`, un autre qui réagit à `ProfileUpdated`, et un contrôleur REST qui répond à `GET /api/matches`. Tous les trois appellent le même service applicatif, à travers ses ports d'entrée. La règle métier "comment calcule-t-on un match" ne bifurque pas selon qui pose la question. C'est le bénéfice pratique des ports : changer d'adapter, garder la règle.

---

## 5. Architecture événementielle avec Kafka

### 5.1 Chorégraphie contre orchestration

Il existe deux façons de coordonner du travail entre services. **L'orchestration** : un processus central appelle chaque étape explicitement, dans l'ordre - "publie la mission, puis dis à Matching de la scorer, puis dis à ApplicationTracking de créer une candidature." **La chorégraphie** : chaque service réagit indépendamment aux events qui l'intéressent, et personne n'est responsable de la séquence globale.

MissionMatch utilise la chorégraphie, de bout en bout. Sourcing publie `MissionPublished` et n'a aucune idée que Matching existe, encore moins qu'il va réagir. Ça reflète les organisations réelles - l'équipe sourcing ne gère pas le workflow de l'équipe matching - et c'est aussi pour ça que les frontières de module tiennent : **le seul moyen légal pour un contexte d'affecter un autre est de publier un event que celui-ci consomme.** Aucun contexte n'importe les classes de domaine d'un autre ; grep le code, vous ne trouverez `import com.missionmatch.sourcing.domain.*` nulle part à l'intérieur de `matching`.

### 5.2 Anatomie d'un flux d'events, étape par étape

Suivez une mission de "publiée" à "matchée" :

**1. Sourcing construit et publie l'event** (extrait de `MissionApplicationService.publish()`, section 4.5) :

```kotlin
missionEventPublisher.publish(
    MissionPublished(missionId = mission.id, requiredSkills = mission.requiredSkills.skills, /* ... */),
)
```

**2. L'adapter conduit le dépose sur Kafka :**

```kotlin
// KafkaMissionEventPublisher.kt
override fun publish(event: DomainEvent) {
    val topic = topicFor(event)   // MissionPublished -> "mission-published"
    val key = keyFor(event)       // l'id de la mission, en chaîne de caractères
    kafkaTemplate.send(topic, key, event)
}
```

La **clé** compte : Kafka ne garantit l'ordre qu'*au sein d'une partition*, et les messages portant la même clé atterrissent toujours sur la même partition. Utiliser `missionId` comme clé signifie que tous les events concernant la même mission sont traités dans l'ordre, les uns par rapport aux autres - mais, ce qui est important, pas forcément dans l'ordre par rapport aux events d'autres topics (la section 6.2 est un vrai bug causé exactement par ça).

**3. L'adapter conducteur de Matching (un consumer Kafka) le reçoit :**

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

**4. Le service applicatif de Matching réagit** : il enregistre sa propre copie (*snapshot*) de la mission, puis la score par rapport à tous les profils freelances connus, en publiant `MatchComputed` pour tout ce qui dépasse le seuil d'éligibilité (voir `MatchingApplicationService.handle(MissionPublishedCommand)` et `MatchingPolicy.score()` en section 3.7).

### 5.3 La couche anti-corruption : pourquoi Matching ne réutilise pas simplement la classe d'event de Sourcing

Regardez de près l'étape 3 ci-dessus : le consumer désérialise vers `MissionPublishedIntegrationEvent` - une classe définie *à l'intérieur du module `matching`*, pas le `MissionPublished` de Sourcing. C'est délibéré, et ça s'appelle une **couche anti-corruption** :

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

Si Matching désérialisait directement vers la classe `MissionPublished` de Sourcing, le build de Matching devrait dépendre du module de Sourcing - réintroduisant exactement le couplage que les contextes bornés sont censés éliminer, et signifiant qu'un changement de la forme interne de l'event de Sourcing pourrait casser la compilation de Matching, alors même que les deux équipes (conceptuellement) ne se parlent jamais. À la place, Matching définit son propre DTO minimal, façonné seulement pour ce dont *lui* a besoin (remarquez : le champ `startDate` n'est utilisé par aucun calcul de scoring de Matching, même s'il est présent - conservé ici pour une future fonctionnalité, sans risque dans un sens comme dans l'autre). Le format d'échange est le *contrat* ; chaque côté possède sa propre traduction de ce contrat en code qu'il maîtrise.

Le même patron se répète pour `MissionId`, `FreelancerId` : Matching définit ses *propres* objets-valeur `MissionId` et `FreelancerId` (dans `matching/domain/`), distincts du `MissionId` de Sourcing et du `FreelancerId` de FreelancerProfile. Ils enveloppent tous un `UUID`, par hasard, mais ce sont des types différents que le compilateur ne vous laissera pas confondre - vous ne pouvez pas passer accidentellement un `MissionId` de Sourcing là où un `MissionId` de Matching est attendu. La notion d'"identité d'une mission" de chaque contexte est définie indépendamment, exprès.

### 5.4 Faire passer du JSON à travers la frontière anti-corruption : des alias de type, pas des noms de classe

Il y a une subtilité pour faire fonctionner réellement la couche anti-corruption sur le fil. Le `JsonSerializer`/`JsonDeserializer` de Spring Kafka doivent s'accorder, d'une manière ou d'une autre, sur le type Kotlin qu'un payload JSON doit devenir. L'approche naïve - intégrer le nom de classe Java pleinement qualifié du producteur dans un en-tête du message Kafka - annulerait tout l'intérêt : la désérialisation du consumer dépendrait alors de la connaissance du nom de classe *du producteur*, recréant le couplage qu'on vient justement d'éliminer.

La solution utilisée dans `application.yml` est un **mapping d'alias de type**, convenu des deux côtés via une courte chaîne de caractères, pas un nom de classe :

```yaml
# Côté producteur (Sourcing, Matching, FreelancerProfile publient tous via cette même config,
# puisqu'ils partagent un seul processus déployable - voir section 8) :
spring.json.type.mapping: >
  mission-published:com.missionmatch.sourcing.domain.event.MissionPublished,
  mission-closed:com.missionmatch.sourcing.domain.event.MissionClosed,
  match-computed:com.missionmatch.matching.domain.event.MatchComputed,
  profile-updated:com.missionmatch.freelancerprofile.domain.event.ProfileUpdated

# Côté consommateur (Matching, puisque c'est lui qui consomme ces trois topics) :
spring.json.type.mapping: >
  mission-published:com.missionmatch.matching.infrastructure.adapter.input.messaging.MissionPublishedIntegrationEvent,
  mission-closed:com.missionmatch.matching.infrastructure.adapter.input.messaging.MissionClosedIntegrationEvent,
  profile-updated:com.missionmatch.matching.infrastructure.adapter.input.messaging.ProfileUpdatedIntegrationEvent
```

Lisez ça attentivement : **l'alias `mission-published` correspond à une classe Kotlin *différente* de chaque côté.** Le producteur associe l'alias à *son propre* event de domaine ; le consumer associe le *même alias* à son propre DTO anti-corruption. Kafka transporte l'alias `mission-published` dans un en-tête de message (`__TypeId__`), et chaque côté résout indépendamment cet alias vers le type local qui lui convient. Aucun des deux côtés n'a besoin de savoir que la classe de l'autre existe seulement. C'est la couche anti-corruption, rendue vraiment fonctionnelle sur un protocole de communication, pas seulement sur un diagramme.

---

## 6. Récits de guerre : de vrais bugs distribués rencontrés sur ce projet

Cette section diffère du reste du guide : ce n'est pas la description d'un patron appliqué correctement dès le départ. Ce sont de vrais bugs, trouvés en faisant *réellement tourner* le système complet - vrai Kafka, vrai Postgres, vrais consumers concurrents - puis corrigés. Ils sont inclus car lire sur la "cohérence à terme" et la "livraison au moins une fois" dans l'abstrait ne prépare pas à ce que ça donne réellement quand ça mord. Les trois sont documentés plus en détail dans l'historique git du projet et dans le [README](../../README.md#event-driven-communication).

### 6.1 Les en-têtes de type, ou la pierre de Rosette manquante

**Symptôme :** les consumers Kafka de Matching levaient `IllegalStateException: No type information in headers and no default type provided` pour chaque message.

**Cause :** la première version de la config producteur désactivait complètement les en-têtes de type (`spring.json.add.type.headers: false`), en pensant "le consumer connaît déjà le type de son propre DTO, pourquoi envoyer l'info de type ?" Mais le `JsonDeserializer` de Spring Kafka a besoin de *quelque chose* dans le message pour décider vers quelle classe désérialiser, et avec les en-têtes désactivés et aucun type par défaut configuré, il n'avait rien sur quoi s'appuyer.

**Correctif :** le mapping d'alias de type décrit en section 5.4 - les en-têtes restent actifs, mais ils transportent un alias court et stable plutôt qu'un nom de classe pleinement qualifié.

### 6.2 Un objet-valeur qui se sérialisait en objet, pas en valeur

**Symptôme :** la désérialisation échouait avec `Cannot deserialize value of type java.util.UUID from Object value (token START_OBJECT)`.

**Cause :** `MissionId` est une `data class MissionId(val value: UUID)`. Par défaut, Jackson sérialise une data class en *objet* JSON : `{"value": "l-uuid"}`. Mais le DTO anti-corruption côté consommateur déclare `missionId: UUID` - un simple scalaire. Jackson s'étranglait en essayant de faire tenir un objet dans un champ UUID.

**Correctif :** annoter le champ enveloppé avec `@JsonValue` de Jackson :

```kotlin
data class MissionId(@get:JsonValue val value: UUID)
```

Ça dit à Jackson : "lors de la sérialisation de ce type, utilise *la valeur de cette propriété* directement comme représentation, pas un objet qui l'enveloppe." `MissionId` se sérialise désormais en simple chaîne UUID sur le fil - conforme à ce que les DTO anti-corruption de l'autre côté attendent réellement.

### 6.3 La session Hibernate était déjà fermée

**Symptôme :** `LazyInitializationException: failed to lazily initialize a collection of role: MissionEntity.requiredSkills - no Session`, mais seulement en appelant le vrai endpoint HTTP, jamais dans le test d'intégration au niveau du repository.

**Cause :** `spring.jpa.open-in-view: false` est configuré délibérément (le laisser à `true`, la valeur par défaut de Spring Boot, est un anti-pattern bien connu : ça garde une connexion à la base de données ouverte pendant toute la requête HTTP, y compris le rendu de la vue, ce qui ne passe pas à l'échelle). Mais les champs `@ElementCollection` (comme `requiredSkills`) sont en `FetchType.LAZY` par défaut. La session étant fermée dès que l'appel au repository retourne, accéder à `mission.requiredSkills` plus tard, en construisant la réponse JSON, tombait sur une collection jamais réellement chargée, sur une session qui n'existait plus.

Le test au niveau du repository ne l'a pas détecté car `@DataJpaTest` enveloppe chaque test dans une transaction qui reste ouverte pendant toute la méthode de test, si bien que la collection paresseuse se chargeait sans broncher - masquant exactement l'échec qu'une vraie requête, avec sa session de courte durée, allait rencontrer.

**Correctif :** marquer la collection en `FetchType.EAGER`. Pour une valeur aussi petite et toujours nécessaire en même temps que la mission elle-même, le chargement immédiat est le bon choix - le chargement paresseux existe pour différer des données *coûteuses et optionnelles*, et `requiredSkills` n'est ni l'un ni l'autre.

### 6.4 Des matches dupliqués à cause d'une course entre deux threads consommateurs

**Symptôme :** la même paire `(missionId, freelancerId)` apparaissait deux fois dans `match_results`, avec le même score, calculé à quelques millisecondes d'écart.

**Cause :** `mission-published` et `profile-updated` sont deux topics Kafka différents, consommés par deux threads d'écoute indépendants. Publier plusieurs missions puis mettre à jour immédiatement après un profil qui correspondait à plusieurs d'entre elles a permis aux deux threads consommateurs de calculer un match pour la *même* paire mission-profil à un instant quasi identique - une course classique de type "vérifier-puis-agir" (*time-of-check-to-time-of-use*, TOCTOU) : les deux threads ont vérifié "un match existe-t-il déjà pour cette paire ?", les deux ont obtenu "non", et les deux ont inséré.

**Correctif :** une vérification-puis-insertion au niveau applicatif ne peut jamais complètement fermer cette course entre threads - seule la base de données peut le garantir. Une contrainte d'unicité sur `(mission_id, freelancer_id)` fait que la base de données rejette la seconde insertion, et l'adapter de repository traite cet échec précis comme "quelqu'un d'autre a déjà enregistré ceci", en récupérant et en retournant la ligne du gagnant plutôt que de propager une erreur :

```kotlin
override fun save(matchResult: MatchResult): MatchResult =
    try {
        jpaRepository.save(MatchResultEntity.fromDomain(matchResult)).toDomain()
    } catch (violation: DataIntegrityViolationException) {
        jpaRepository.findByMissionIdAndFreelancerId(matchResult.missionId.value, matchResult.freelancerId.value)
            ?.toDomain() ?: throw violation
    }
```

### 6.5 Une mission fermée avant même d'exister, du point de vue de Matching

**Symptôme :** fermer une mission dans Sourcing n'avait aucun effet sur la copie qu'en avait Matching - elle restait "ouverte" pour toujours, même des minutes plus tard.

**Cause, première hypothèse (fausse) :** "peut-être que l'event `MissionClosed` est retraité *après* `MissionPublished`, annulant la fermeture." Ça semblait raisonnable, et c'était faux - vérifié comme faux en ajoutant des logs temporaires et en lisant le véritable ordre des events.

**Cause, en réalité :** `mission-published` et `mission-closed` sont, encore, deux topics différents. Kafka donne des garanties d'ordre *à l'intérieur* d'une partition de topic, jamais *entre* des topics. Chaque thread consommateur devient "prêt" (termine sa propre affectation de partition / son rééquilibrage) indépendamment. Dans l'exécution observée, le consumer de `mission-closed` est devenu prêt et a traité son event *avant même que* le consumer de `mission-published` ait commencé - donc `MissionClosed` est arrivé sur un service qui n'avait encore aucun snapshot à fermer, n'a rien trouvé, et n'a correctement rien fait (selon la logique de l'époque). Puis `MissionPublished` est arrivé et a créé un snapshot tout neuf, par défaut ouvert - car, autant que ce handler pouvait le savoir, cette mission n'avait jamais été fermée.

**Correctif :** ne pas du tout se fier à l'ordre d'arrivée. Enregistrer le fait "cette mission est fermée" indépendamment de l'existence d'un snapshot, dans sa propre petite table :

```kotlin
override fun handle(command: MissionClosedCommand) {
    missionSnapshotRepository.markClosed(command.missionId)          // toujours, sans condition
    val mission = missionSnapshotRepository.findById(command.missionId) ?: return
    missionSnapshotRepository.save(mission.copy(open = false))
}

override fun handle(command: MissionPublishedCommand) {
    val open = when {
        missionSnapshotRepository.isMarkedClosed(command.missionId) -> false
        else -> missionSnapshotRepository.findById(command.missionId)?.open ?: true
    }
    // ... construire et sauvegarder le snapshot avec cette valeur `open`
}
```

C'est correct quel que soit l'event qui arrive en premier, car aucun des deux handlers ne suppose quoi que ce soit sur l'ordre - chacun vérifie simplement le fait durable et agit en conséquence. La leçon, énoncée plus généralement : **quand deux events sont liés causalement mais voyagent sur des topics différents, ne corrigez pas le symptôme en ajustant le timing (un délai, une pause) ; corrigez le modèle pour que la justesse ne dépende plus du tout de l'ordre d'arrivée.**

---

## 7. Test-Driven Development & Behavior-Driven Development

### 7.1 La pyramide des tests, appliquée

Beaucoup de tests rapides et étroits en bas ; moins de tests lents et larges en haut. La version de cette pyramide dans ce projet se superpose directement aux couches hexagonales :

| Couche | Outillage | Ce qui est réel, ce qui est simulé |
|---|---|---|
| Domaine | JUnit 5, AssertJ | Tout est réel - il n'y a aucune infrastructure à simuler |
| Application | JUnit 5, Mockito, AssertJ | Domaine réel, ports de sortie mockés |
| Adapters d'infrastructure | JUnit 5, Testcontainers, AssertJ | Base de données/broker réels (conteneurisés), rien n'est mocké |

### 7.2 Couche domaine : aucun mock nécessaire, et le TDD y est le plus facile

Le **TDD (Test-Driven Development)** est un cycle : écrire un test qui échoue pour un comportement qui n'existe pas encore, écrire le minimum de code pour le faire passer, puis refactoriser avec le filet de sécurité de ce test qui passe. C'est le plus facile à pratiquer dans la couche domaine précisément parce qu'il n'y a rien à installer - pas de base de données, pas de HTTP, pas de mocks - juste une fonction ou un objet pur et une assertion.

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

Ce test aurait été écrit *avant* que la ligne `check(status == MissionStatus.OPEN)` n'existe, dans un vrai flux TDD : écrire ce test (il échoue, car rien n'empêche encore un second appel à `close()`), puis ajouter le `check(...)` pour le faire passer.

### 7.3 BDD : Given / When / Then, partout

Le **BDD (Behavior-Driven Development)** recentre les tests sur le comportement observable - état de départ, action, résultat observable - plutôt que sur les étapes d'implémentation. Chaque test de ce code, à chaque couche, est structuré avec des commentaires `// Given`, `// When`, `// Then` marquant ces trois sections, qu'un framework BDD comme Cucumber soit impliqué ou non. La seule convention apporte déjà l'essentiel du bénéfice de clarté : en lisant le test ci-dessus, vous n'avez besoin de rien savoir sur `Mission` pour comprendre *ce qui est vérifié* - une mission déjà fermée, fermée à nouveau, lève une exception.

### 7.4 Couche application : mocker les ports, jamais le domaine

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

`profileSnapshotRepository` et `matchResultRepository` sont des mocks Mockito, de vrais substituts en mémoire pour les *ports de sortie* (des interfaces). `MatchingPolicy`, le vrai service de domaine, n'est pas mocké ; il s'exécute réellement à l'intérieur de `service.handle(command)`, en faisant de vrais calculs de recouvrement de compétences. C'est le bénéfice pratique de la règle de dépendance de la section 4, rendu concret : parce que `MatchingApplicationService` ne dépend que d'*interfaces* de ports, un test peut substituer n'importe quoi satisfaisant cette interface, y compris un stub Mockito de cinq lignes, et malgré tout exercer de la vraie logique métier sans aucune infrastructure.

### 7.5 Adapters d'infrastructure : Testcontainers, parce que mocker une base de données ne prouve rien

Les tests unitaires ne peuvent structurellement pas détecter un mauvais mapping SQL, une incohérence de sérialisation Kafka, ou un index manquant - parce que dans un test unitaire, il n'y a pas de vraie base de données ou de broker contre lesquels avoir tort. **Testcontainers** résout ça en faisant tourner le vrai moteur (Postgres, Kafka) dans un conteneur Docker jetable, le temps du test :

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

`@AutoConfigureTestDatabase(replace = Replace.NONE)` est importante et facile à manquer : par défaut, `@DataJpaTest` remplace silencieusement la base par une base embarquée en mémoire, ce qui annulerait tout l'intérêt de ce test (une base en mémoire n'a pas les particularités réelles du dialecte SQL de Postgres, ni son comportement de contraintes, ni sa sémantique de connexions). Cette annotation dit "non - utilise le vrai conteneur Postgres que je viens de démarrer." Aucun des bugs de la section 6 (ceux de désérialisation JSON, la `LazyInitializationException`, la course sur la contrainte d'unicité) n'aurait été détecté par des mocks ; plusieurs n'ont été détectés qu'en faisant tourner la *pile réelle dans son ensemble*, un niveau de réalisme au-delà même de ce que Testcontainers seul apporte - Testcontainers rend fiable le test d'intégration d'un seul adapter, mais seul faire tourner le système entier détecte les bugs qui vivent dans l'interaction *entre* les adapters.

---

## 8. Visite guidée de la structure du code

### 8.1 Niveau racine

```
missionmatch/
├── backend/            # Kotlin/Spring Boot, un module Gradle par contexte borné
├── frontend/            # Angular
├── docker-compose.yml    # Postgres + Kafka pour le développement local
└── docs/                 # ce guide
```

### 8.2 À l'intérieur d'un module backend

Chaque contexte implémenté (`sourcing`, `matching`, `freelancer-profile`) suit la même forme interne, c'est pourquoi la visite guidée de `sourcing` en section 4.5 vous apprend aussi à lire `matching` et `freelancer-profile` :

```
<contexte>/
├── domain/
│   ├── <Agrégat>.kt
│   ├── <ObjetValeur>.kt
│   └── event/
│       └── <EventDeDomaine>.kt
├── application/
│   ├── port/
│   │   ├── input/<UseCase>.kt
│   │   └── output/<Repository/Publisher>.kt
│   └── <Contexte>ApplicationService.kt
└── infrastructure/
    ├── <Contexte>Configuration.kt          # câble le service applicatif comme bean Spring
    └── adapter/
        ├── input/
        │   ├── web/<Controller>.kt
        │   └── messaging/<Consumer>.kt
        └── output/
            ├── persistence/<Entity/Repository/Adapter>.kt
            └── messaging/<Publisher>.kt
```

Deux fichiers de niveau module méritent d'être connus, car ils ne rentrent dans aucun contexte unique :

- `shared-kernel/` - voir section 3.8.
- `bootstrap/` - l'unique application Spring Boot qui assemble tous les modules de contexte en un seul processus déployable (voir "Monolithe modulaire" ci-dessous). Il contient `application.yml` (toute la configuration, y compris les mappings de type Kafka de la section 5.4) et `DemoDataSeeder.kt` (un composant activé par `@Profile("demo")` qui peuple des données de démonstration réalistes en appelant les *vrais* use cases - publier des missions, mettre à jour un profil - si bien que les données de démo passent exactement par le même chemin de code que les actions d'un vrai utilisateur, events compris).

### 8.3 Pourquoi un seul processus déployable, pas cinq microservices

MissionMatch est livré comme une seule application Spring Boot - un **monolithe modulaire** (*modular monolith*) : chaque contexte borné est son propre module Gradle avec ses propres couches hexagonales, mais ils tournent tous dans le même processus JVM. C'est un choix délibéré pour un projet d'apprentissage : ça donne une pratique complète des frontières de module DDD et une *vraie* messagerie Kafka entre contextes, sans le coût opérationnel de déployer, superviser et versionner cinq services séparés.

Les frontières de module sont appliquées assez strictement - aucun import inter-module des classes `domain` ou `application`, communication uniquement via des events Kafka (section 5) ou du REST - pour que sortir n'importe quel module en microservice à part plus tard soit un changement de déploiement, pas une refonte. C'est le vrai test pour savoir si un "monolithe modulaire" est réellement modulaire : pourriez-vous extraire un module sans toucher son code, seulement son packaging ?

---

## 9. Le frontend, concept par concept

Le frontend n'est pas juste une interface posée sur les concepts backend ci-dessus - il applique son propre petit ensemble de patrons délibérés, qui méritent d'être nommés.

**Des composants autonomes (*standalone*), pas de NgModules.** Chaque composant (`MissionList`, `ChipInput`, `Sidebar`, ...) déclare ses propres dépendances via un tableau `imports: [...]` dans son décorateur `@Component`, plutôt que d'être enregistré dans un module partagé. Ça garde la liste des dépendances d'un composant locale et explicite - lire le haut de `mission-list.ts` vous dit tout ce dont il a besoin, sans rien de sous-entendu par le module qui l'aurait déclaré.

**Des signaux pour l'état, pas des sujets RxJS.** Un état comme `MissionList.missions` est un `signal<Mission[]>([])` d'Angular, lu de façon réactive dans le template (`missions()`), mis à jour avec `.set()`/`.update()`. C'est l'idiome Angular moderne pour l'état local d'un composant - plus simple qu'un `BehaviorSubject` RxJS pour un état qui n'a pas besoin d'opérateurs de flux comme `debounceTime` ou `switchMap`.

**Un composant réutilisable construit avec `model()` pour le binding bidirectionnel.** `ChipInput` (l'éditeur de tags de compétences utilisé à la fois par le formulaire de mission et la page de profil) expose sa liste de compétences comme `readonly skills = model.required<string[]>()`, permettant à un parent de faire `[(skills)]="monSignalDeCompetences"` - l'équivalent, basé sur les signaux, du `[(ngModel)]` d'Angular, généralisé à l'état propre de n'importe quel composant, pas seulement aux champs de formulaire.

**Pas de backend pour l'authentification, donc le frontend invente une identité locale stable.** Il n'y a pas encore de système de connexion. `shared/local-freelancer-id.ts` génère un UUID avec `crypto.randomUUID()` à la première visite et le stocke dans `localStorage`, si bien que le même navigateur est reconnu comme "le même freelance" d'une visite à l'autre - utilisé par la page Profil (pour savoir de qui créer/mettre à jour le profil) et la page Matches (pour préremplir "de qui je regarde les matches"). C'est un palliatif pragmatique, explicitement pas un mécanisme de sécurité, pour un concept ("qui est l'utilisateur actuel") qu'un vrai système résoudrait avec une authentification effective.

**Un proxy, pas du CORS, pour le développement local.** `frontend/proxy.conf.json` redirige toute requête vers `/api/*` depuis le serveur de développement Angular (port 4310) vers le backend (port 8181). Ça fait que le navigateur voit tout comme provenant de la même origine pendant le développement, contournant complètement le CORS pour ce flux de travail (le backend a aussi une politique CORS explicite configurée, pour le cas où on l'atteindrait directement sans passer par le proxy).

---

## 10. Glossaire

- **Adapter conducteur** (*driving adapter*) - un adapter qui initie un appel vers la couche application. *Exemple : `MissionController` (REST), `MissionPublishedConsumer` (Kafka).*
- **Adapter conduit** (*driven adapter*) - un adapter que la couche application appelle pour réaliser un port de sortie. *Exemple : `MissionRepositoryAdapter` (JPA), `KafkaMissionEventPublisher`.*
- **Agrégat** (*aggregate*) - un ensemble d'objets de domaine traité comme une seule unité pour les changements de données, avec une **racine d'agrégat** comme unique point d'entrée que le code externe peut appeler. Garantit que les invariants ne sont jamais laissés incohérents. *Exemple : `Mission`.*
- **BDD (Behavior-Driven Development)** - décrire et tester le comportement d'un point de vue observable et lisible par le métier (Given/When/Then) plutôt que par les étapes d'implémentation.
- **Chorégraphie** (contre orchestration) - un style d'intégration où chaque service réagit indépendamment aux events, sans coordinateur central décidant de la séquence.
- **Cohérence à terme** (*eventual consistency*) - la garantie qu'en l'absence de nouvelles mises à jour, toutes les parties d'un système distribué finiront *par* refléter le même état, mais pas nécessairement au même instant. *Exemple : un freelance peut voir une mission dans Sourcing quelques centaines de millisecondes avant que Matching ne l'ait scorée.*
- **Contexte borné** (*bounded context*) - une frontière à l'intérieur de laquelle un modèle de domaine et un vocabulaire précis sont cohérents et non ambigus ; le même mot peut signifier autre chose dans un autre contexte. *Exemple : "Profile" ne veut pas dire la même chose dans `FreelancerProfile` (un CV complet) et dans `Matching` (un `ProfileSnapshot` avec juste les compétences et le tarif).*
- **Couche anti-corruption** (*anti-corruption layer*) - une frontière de traduction qui empêche le modèle interne d'un contexte borné de fuiter dans un autre. *Exemple : `MissionPublishedIntegrationEvent` dans `matching`, une copie locale du format d'échange qui ne dépend pas de la classe `MissionPublished` de Sourcing.*
- **Entité** (*entity*) - un objet défini par son identité (un identifiant), pas par ses attributs actuels, dont l'état peut changer dans le temps. *Exemple : `Mission`.*
- **Event de domaine** (*domain event*) - un fait qui s'est produit dans le domaine, nommé au passé, auquel d'autres parties du système peuvent réagir. *Exemple : `MissionPublished`.*
- **Idempotence** - la propriété qu'exécuter la même opération (ou traiter le même event) plus d'une fois produit le même résultat que l'exécuter une seule fois, sans effet de bord supplémentaire. *Exemple : la contrainte d'unicité sur `match_results` qui transforme une insertion en doublon en un no-op sûr plutôt qu'en une ligne dupliquée.*
- **Langage ubiquitaire** (*ubiquitous language*) - le vocabulaire partagé entre les développeurs et les experts métier à l'intérieur d'un contexte borné, utilisé littéralement dans le code - noms de classes, noms de méthodes - pas seulement dans des commentaires ou de la documentation.
- **Monolithe modulaire** (*modular monolith*) - une application unique déployable, découpée en interne en modules strictement délimités (ici, un par contexte borné), communiquant uniquement par les mêmes types de frontières (events, ports) qu'un véritable découpage en microservices utiliserait.
- **Noyau partagé** (*shared kernel*) - un petit morceau de modèle, explicitement accepté d'un commun accord, partagé entre plusieurs contextes bornés, gardé minimal car chaque contexte qui en dépend doit donner son accord avant qu'il change. *Exemple : `Money`, `SkillSet` dans `shared-kernel`.*
- **Objet-valeur** (*value object*) - un objet entièrement défini par ses attributs, immuable, sans identité ; deux objets-valeur portant les mêmes données sont interchangeables. *Exemple : `Money`, `SkillSet`.*
- **Architecture hexagonale** (*Ports & Adapters*) - une architecture où la logique métier (domaine + application) ne dépend de rien d'externe ; toute la technologie vit dans des adapters qui implémentent ou appellent des interfaces (ports) définies par la logique métier.
- **Port** - une interface, possédée par la couche application, nommant une capacité requise (port de sortie) ou offerte (port d'entrée), indépendante de toute technologie précise. *Exemple : `MissionRepository` (sortie), `PublishMissionUseCase` (entrée).*
- **Service de domaine** (*domain service*) - de la logique de domaine qui n'appartient naturellement à aucune entité ni objet-valeur unique, car elle a besoin de deux d'entre eux à la fois. *Exemple : `MatchingPolicy`.*
- **Service applicatif** (*application service*) - orchestre les objets de domaine et les ports de sortie pour réaliser un cas d'usage ; ne contient aucune règle métier propre. *Exemple : `MissionApplicationService`.*
- **TDD (Test-Driven Development)** - écrire un test qui échoue avant le code qui le fait passer, puis refactoriser, en cycles courts et répétés.
- **Testcontainers** - une bibliothèque qui fait tourner de vraies dépendances (bases de données, brokers) comme conteneurs Docker jetables pendant l'exécution d'un test, pour que les tests d'intégration exercent le vrai moteur plutôt qu'un mock.

---

*[Read in English](../en/ARCHITECTURE.md) · [Retour au README](../../README.md)*
