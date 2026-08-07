# Décisions d'architecture

Ce document trace les choix structurants du codebase et leurs compromis — utile tel quel
comme base de la section « choix techniques » du mémoire.

## 1. Une seule instance PostgreSQL, plusieurs bases

**Choix** : `docker-compose.yml` ne lance qu'un conteneur `postgres`, dans lequel
`infra/postgres-init/01-create-databases.sh` crée une base par service (`user_db`,
`space_db`, ...).

**Raison** : contrainte matérielle (Intel i5-6200U, 8 Go RAM, pas de GPU). Sept conteneurs
PostgreSQL séparés consommeraient inutilement de la RAM pour un gain d'isolation marginal
en développement local.

**Ce qui est préservé** : chaque service reste propriétaire exclusif de sa base au niveau
applicatif (aucune requête cross-service, aucune foreign key inter-base). Le passage à un
Postgres par service en production ne nécessite qu'un changement de `DB_URL`.

## 2. Authentification centralisée à la gateway

**Choix** : `api-gateway` est le seul point de vérification du JWT. Il enrichit chaque
requête avec `X-User-Id`, `X-User-Role`, `X-Request-Id`, headers auxquels les services
backend font confiance sans revalider le token.

**Raison** : évite de dupliquer la logique JWT (clé de signature, parsing, gestion des
erreurs) dans 7 services. Simplifie aussi l'ajout d'un rate limiting cohérent en un seul
endroit.

**Compromis assumé** : un service backend est intrinsèquement vulnérable si accédé
directement (hors gateway) sans passer par la vérification JWT. En développement local,
les services restent joignables directement (utile pour Swagger/tests), donc ce risque
n'existe qu'en environnement réseau non cloisonné — à documenter comme limite connue.

## 3. Cascade de suppression par événements, jamais par appel synchrone

**Choix** : la suppression d'un espace (`SPACE_DELETED`) ou d'un compte (`USER_DELETED`)
déclenche un événement Redis Pub/Sub consommé indépendamment par chaque service concerné,
plutôt qu'une chaîne d'appels REST synchrones depuis le service à l'origine de la
suppression.

**Raison** : découplage total. `space-service` n'a pas besoin de connaître l'existence
d'`ingestion-service`, `chat-service`, etc. pour fonctionner ; ajouter un futur service
consommateur ne nécessite aucune modification de `space-service`.

**Compromis assumé** : cohérence éventuelle (eventual consistency), pas immédiate. Un
espace supprimé peut laisser des documents orphelins pendant quelques centaines de
millisecondes le temps que Redis propage l'événement — acceptable pour ce cas d'usage
(pas de contrainte temps réel dure).

## 4. Module `common` : lib partagée, pas un service

**Choix** : `common` est un JAR Maven classique (pas de `spring-boot-maven-plugin` avec
repackage), consommé comme dépendance par les 8 services applicatifs.

**Raison** : évite la duplication de l'enveloppe de réponse, de la gestion d'erreurs, du
parsing des headers de contexte utilisateur et des DTOs d'événements dans chaque service —
sans pour autant introduire un service réseau supplémentaire (donc aucun coût de latence
ni de disponibilité).

**Mécanisme technique notable** : l'enregistrement automatique du filtre de contexte et du
gestionnaire d'exceptions global se fait via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
(mécanisme d'auto-configuration standard Spring Boot depuis 2.7), plutôt que par scan de
composants — nécessaire car les classes de `common` vivent dans un package distinct
(`mg.esmia.miage.common`) de celui de chaque service.

## 5. Découpage volontairement inégal entre CRUD générique et cœur IA

**Choix** : `user-service`, `analytics-service` et `gamification-service` sont livrés
complets. `space-service` et `fiche-service` ont un CRUD complet mais un point d'extension
IA isolé (`PersonaService`, `FicheGenerationService`). `ingestion-service` a son **pipeline
d'ingestion complet** (extraction docling-worker + vision Gemini, chunking orienté sens par
titres — `MarkdownChunkingService` —, embeddings, indexation Qdrant), chaque étape déléguée
à un service dédié orchestré par `IngestionPipelineService`. `chat-service` a son
**orchestration RAG câblée** : `QdrantVectorStore` (collection unique `chunks`, filtre
`space_id` en payload) + `QuestionAnswerAdvisor`, persona via `SpaceClient` (appel interne
à `space-service`, headers `X-User-Id` reproduits), LLM via `LlmProviderConfig` +
`ChatProviderResolver`. Il reste la validation de bout en bout et le persona réel de
`space-service`.

**Raison** : c'est la partie du travail qui a une réelle valeur pour un mémoire M1 — la
plomberie CRUD/Docker/événements n'a pas besoin d'être réinventée.

## 6. Bascule de provider LLM par configuration

**Choix** : `ACTIVE_LLM_PROVIDER` (`groq` | `gemini` | `ollama`) piloté par variable
d'environnement, avec Groq intégré via le starter Spring AI OpenAI pointé sur son
`base-url` compatible OpenAI, et Ollama comme fallback local pour les démonstrations
hors-ligne (soutenance sans connexion, ou machine sans clé API).

Chaque starter auto-configure son `ChatModel` (`OpenAiChatModel` / `OllamaChatModel`) ;
`LlmProviderConfig` expose un `ChatClient` qualifié par provider et `ChatProviderResolver`
centralise la sélection au runtime (`.current()`, avec repli non bloquant si le provider
configuré n'est pas enregistré) — `ChatService` l'utilise pour l'appel LLM.

**Point ouvert** : l'intégration Gemini AI Studio n'a pas de starter Spring AI officiel
identifié avec certitude au moment de la génération de ce codebase (Spring AI propose un
starter Vertex AI Gemini, différent de l'API AI Studio à clé simple). Piste privilégiée :
**seconde instance du starter OpenAI** (bean `OpenAiApi`/`OpenAiChatModel` manuel pointé sur
l'endpoint compatible Gemini, même mécanisme que `docling-worker`) ajoutée à la `Map` du
`ChatProviderResolver` — à trancher au premier build.

## 7. Limites connues de ce codebase généré

- **Concurrence d'ingestion non bornée** : `processAsync` (@Async) s'appuie sur le
  `SimpleAsyncTaskExecutor` par défaut — un thread par upload, aucun pool, et chaque
  conversion spawné un conteneur `docling-worker` (docker-java). Sous charge, N uploads
  simultanés = N threads + N conteneurs + N×M appels Gemini (quota RPM) → risque de
  saturation RAM/CPU en local et de dépassement de quota en serveur. Analyse et pistes de
  correction (pool borné, sémaphore de conteneurs, idempotence par document, retry/backoff)
  documentées dans [`docs/ingestion-concurrence.md`](docs/ingestion-concurrence.md).
- Versions de dépendances non validées par une build réelle (pas d'accès à Maven Central
  depuis l'environnement de génération) — à vérifier au premier `mvn install`.
- `extractNotion()` dans `analytics-service` est une heuristique lexicale simple (premier
  mot significatif hors mots vides), pas une extraction sémantique — suffisante pour
  peupler les tableaux de bord dès le départ, améliorable par la suite.
- `FicheEvent.validated()` ne porte que l'identifiant de l'enseignant, pas celui de
  l'étudiant auteur de la fiche : `analytics-service` et `gamification-service` ne peuvent
  donc pas encore relier une validation à la progression exacte de l'étudiant concerné —
  documenté en commentaire dans le code, corrigible en enrichissant l'événement côté
  `fiche-service`.
