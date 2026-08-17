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
complets. `space-service` et `fiche-service` ont un CRUD complet et un point d'extension
IA isolé (`PersonaService`, `FicheGenerationService`) désormais **implémentés** (LLM via
`ai-common`, §6.1). `ingestion-service` a son **pipeline d'ingestion complet** (extraction
docling-worker + vision Gemini, chunking orienté sens par titres — `MarkdownChunkingService` —,
embeddings, indexation Qdrant), chaque étape déléguée à un service dédié orchestré par
`IngestionPipelineService`. `chat-service` a son **orchestration RAG câblée** : `QdrantVectorStore`
(collection unique `chunks`, filtre `space_id` en payload) + pipeline custom `RagPipelineAdvisor`
(§6.3), persona via `SpaceClient` (appel interne à `space-service`, headers `X-User-Id`
reproduits), LLM via `ai-common` (`LlmProviderAutoConfiguration` + `ChatProviderResolver`).
Il reste la validation de bout en bout avec l'infra complète (Ollama/Qdrant) et le test live
Gemini.

**Raison** : c'est la partie du travail qui a une réelle valeur pour un mémoire M1 — la
plomberie CRUD/Docker/événements n'a pas besoin d'être réinventée.

## 6. IA : provider, pipeline RAG et décisions Spring AI

### 6.1 Bascule de provider LLM par configuration

**Choix** : `ACTIVE_LLM_PROVIDER` (`groq` | `gemini` | `ollama`) piloté par variable
d'environnement, avec Groq intégré via le starter Spring AI OpenAI pointé sur son
`base-url` compatible OpenAI, et Ollama comme fallback local pour les démonstrations
hors-ligne (soutenance sans connexion, ou machine sans clé API).

La configuration et la résolution de provider sont **centralisées dans le module `ai-common`**
(§6.6) : les 3 services IA (`chat`, `space`, `fiche`) la réutilisent sans duplication.
`LlmProviderAutoConfiguration` expose un `ChatClient` qualifié par provider
(`groq`/`gemini`/`ollama`) et `ChatProviderResolver` centralise la sélection au runtime :
`current()` renvoie le `ChatClient` du provider actif et **lève
`ApiException(LLM_PROVIDER_UNAVAILABLE, 503)`** si ce provider n'est pas disponible — pas de
repli silencieux : chaque service décide de sa politique de repli (circuit breaker, persona
générique, message d'indisponibilité).

**Gemini** : Google expose une **API compatible OpenAI officielle**
(`https://generativelanguage.googleapis.com/v1beta/openai/`, clé AI Studio, noms de modèles
Gemini) — c'est un simple changement de base-url, comme pour Groq. Contrainte réelle : le bean
auto-configuré `openAiChatModel` est `@ConditionalOnMissingBean(OpenAiApi.class)` (le starter
n'accepte qu'UNE auto-configuration, déjà prise par Groq). Le modèle Gemini est donc construit
via une **seconde instance `OpenAiApi` en variable locale** de la méthode `@Bean` (jamais un
bean Spring), sur le namespace dédié `spring.ai.gemini.*`, avec `completionsPath
/chat/completions` (l'endpoint Gemini n'a pas le segment `/v1` que Spring AI met par défaut).

### 6.2 Spring AI 1.1.8 sur Spring Boot 3.3.5 (pas 2.0)

**Choix** : la version **1.1.8** (dernière de la branche 1.x) est épinglée au POM racine.
La **2.0.0** est stable mais **exige Spring Boot 4.1.0** (ses starters dépendent en dur de
`spring-boot-starter-webclient`/`restclient` 4.1.0 et de `kotlin-reflect` 2.3.21). Migrer le
monorepo (9 modules, Spring Cloud 2025.x, springdoc 3.x, Flyway 11) — dont `ingestion-service`
(interdit) — sortait du périmètre : **on reste en 1.1.8 sur Boot 3.3.5**, et le RAG modulaire
2.0 est remplacé par un pipeline custom équivalent (§6.3).

### 6.3 Pipeline RAG custom (équivalent du RAG modulaire 2.0, absent en 1.1.x)

Le RAG modulaire de Spring AI (`RetrievalAugmentationAdvisor`, `RewriteQueryTransformer`,
`VectorStoreDocumentRetriever`, `ContextualQueryAugmenter`, `DocumentPostProcessor`) n'existe
qu'en **2.0** (module `spring-ai-rag`) ; et le `RerankingDocumentPostProcessor` /
`DocumentReranker` ne sont **même pas livrés en 2.0.0** (vérifié dans les jars). Le spec
autorisait le repli : chat-service implémente donc un **pipeline custom** — `RagPipelineAdvisor`
(un `CallAdvisor` Spring AI 1.1.x) :

1. **Réécriture de requête** (`RewriteQueryTransformer` 2.0) : un appel LLM transforme la
   question brute en requête de retrieval autonome (essentiel en conversation multi-tour où
   « et pour le second cas ? » est sans sens isolé). Échec = question brute.
2. **Retrieval large** (`VectorStoreDocumentRetriever` 2.0) : `topK` volontairement élevé (40)
   avec seuil bas (0.5), filtré par `space_id` en payload — **Option A multi-tenant** : collection
   Qdrant **unique** `chunks`, chaque point porte `space_id` (payload `{document_id, space_id,
   chunk_index, content}`, §3.1 vérifié).
3. **Rerank** (`RerankingDocumentPostProcessor` 2.0) : réduction au `topN` restreint (5) par un
   **second appel LLM** (`LlmDocumentReranker`, tags `[C0]..[Cn]`). **Choix : zéro nouvelle
   dépendance** — pas de provider de reranking dédié (type Cohere/Jina), cohérent avec l'arbitrage
   global « éviter les services externes additionnels ». Échec = les `topN` premiers candidats.
4. **Augmentation du contexte** (`ContextualQueryAugmenter` 2.0) : les documents retenus sont
   injectés dans le prompt système (et exposés en métadonnée `rag_retrieved_documents` — clé
   `RagPipelineAdvisor.RETRIEVED_DOCUMENTS` — pour la traçabilité `retrieved_chunk_ids`). **Anti-hallucination assumé** : si le contexte est vide,
   le modèle est invité à répondre honnêtement qu'il ne trouve pas l'information dans les cours,
   plutôt que d'inventer.

### 6.4 Mémoire de chat : `JpaBackedChatMemory` (une seule source de vérité)

`MessageChatMemoryAdvisor` + `ChatMemory` en base : `JpaBackedChatMemory` implémente
`ChatMemory` par-dessus `MessageRepository` — **aucun store en mémoire Spring AI**, la table
`messages` reste la source de vérité unique (historique durable, partagé entre instances et
redémarrages). `MessageChatMemoryAdvisor.before()` injecte l'historique (fenêtre
`CHAT_MAX_HISTORY_MESSAGES`) ; `after()` persiste les messages. **Double-écriture réconciliée
par idempotence** : `ChatService` persiste le message USER **avant** l'appel (événement
`MESSAGE_CREATED` immédiat, même si le LLM tarde ou échoue), et `JpaBackedChatMemory.add()` est
idempotent (un message dont le contenu correspond au dernier message du même rôle n'est pas
ré-inséré). Le message ASSISTANT persisté par l'advisor est ensuite **enrichi** par
`ChatService` (retrievedChunkIds + modelUsed) et publié ; en fallback du circuit breaker
(l'advisor n'a pas tourné), il est persisté directement.

### 6.5 Résilience des appels LLM

Chaque appel LLM est isolé dans un composant dédié (`ChatLlmService`, `PersonaService`,
`FicheGenerationService`) annoté `@CircuitBreaker` — **composant séparé pour éviter
l'auto-invocation AOP** (l'appel depuis `ChatService` passe bien par le proxy). Instances
`llm-chat`, `llm-persona`, `llm-fiche` (fenêtre glissante 10, seuil 50 %, 5 appels minimum).
Replis :
- `chat-service` → message d'indisponibilité **honnête** (« L'assistant est temporairement
  indisponible »), jamais une réponse statique trompeuse ;
- `space-service` → persona « template » déterministe (génération) ou persona inchangé
  (enrichissement) ;
- `fiche-service` → `ApiException` 503 (génération à relancer), pas de fiche placeholder.

### 6.6 Module `ai-common` : résolveur de provider partagé

Les 3 services IA devaient charger les starters OpenAI + Ollama, la config des 3 providers et
le résolveur — dupliqué (et divergeant) par service. `ai-common` centralise : starters
OpenAI/Ollama, `LlmProviderAutoConfiguration` (beans `ChatClient` qualifiés + resolver
`@ConditionalOnMissingBean`) et `ChatProviderResolver`. Le resolver **lève** une `ApiException`
plutôt que de replier silencieusement sur `ollama` (comportement historique) : la défaillance
devient visible et pilotable par chaque service. Module sans dépendance circulaire (`common`
ne dépend pas de `ai-common`). Les `VectorStore` Qdrant restent **auto-configurés** par les
starters des services (collection `chunks`, `initializeSchema=false` — la collection est créée
par ingestion-service) : l'auto-config est l'équivalent du bean explicite du spec §3.3.

## 7. Limites connues de ce codebase généré

- **Concurrence d'ingestion non bornée** : `processAsync` (@Async) s'appuie sur le
  `SimpleAsyncTaskExecutor` par défaut — un thread par upload, aucun pool, et chaque
  conversion spawné un conteneur `docling-worker` (docker-java). Sous charge, N uploads
  simultanés = N threads + N conteneurs + N×M appels Gemini (quota RPM) → risque de
  saturation RAM/CPU en local et de dépassement de quota en serveur. Analyse et pistes de
  correction (pool borné, sémaphore de conteneurs, idempotence par document, retry/backoff)
  documentées dans [`docs/ingestion-concurrence.md`](docs/ingestion-concurrence.md).
- Versions de dépendances validées par de vraies builds (`mvn clean package` de
  `common`, `ai-common`, `chat-service`, `space-service`, `fiche-service` avec leurs modules
  amont) — build globale à rejouer après toute évolution.
- `extractNotion()` dans `analytics-service` est une heuristique lexicale simple (premier
  mot significatif hors mots vides), pas une extraction sémantique — suffisante pour
  peupler les tableaux de bord dès le départ, améliorable par la suite.
- `FicheEvent.validated()` ne porte que l'identifiant de l'enseignant, pas celui de
  l'étudiant auteur de la fiche : `analytics-service` et `gamification-service` ne peuvent
  donc pas encore relier une validation à la progression exacte de l'étudiant concerné —
  documenté en commentaire dans le code, corrigible en enrichissant l'événement côté
  `fiche-service`.
