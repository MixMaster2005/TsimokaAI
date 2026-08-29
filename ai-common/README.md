# 🧠 ai-common

Librairie **IA partagée** entre les services qui consomment un LLM (`chat-service`,
`space-service`, `fiche-service`). Un seul endroit pour la bascule de provider, pour éviter
de dupliquer la configuration par service et de la faire diverger.

> **Statut :** ✅ Stable — compilée, intégrée aux 3 services IA.

## Rôle

- **`ChatProviderResolver`** : résout le `ChatClient` du provider **actif**
  (`chat.llm.active-provider` : `groq` | `gemini` | `ollama`). Le provider est sélectionné
  par configuration, pas par code.
  - `current()` : renvoie le `ChatClient` du provider actif.
  - `activeProvider()` : nom du provider actif.
  - Si le provider actif n'est pas disponible (non configuré / non enregistré), `current()`
    lève `ApiException(ErrorCode.LLM_PROVIDER_UNAVAILABLE, 503)` — **pas de repli silencieux** :
    chaque service décide de son comportement (fallback métier, circuit breaker).

- **`LlmProviderAutoConfiguration`** (auto-configuration Spring Boot) : construit le bean
  `chatProviderResolver` (`@ConditionalOnMissingBean`) autour de **suppliers paresseux** —
  chaque client est instancié au premier appel, jamais au démarrage :
  - `groq` — `OpenAiApi` + `OpenAiChatModel` construits localement depuis
    `spring.ai.openai.*` (base-url Groq compatible OpenAI).
  - `gemini` — idem, sur l'endpoint officiel compatible OpenAI de Gemini
    (`https://generativelanguage.googleapis.com/v1beta/openai`), avec
    `completionsPath=/chat/completions` (Gemini n'a pas de préfixe `/v1`, contrairement à Groq).
  - `ollama` — `OllamaChatModel` auto-configuré par son starter, injecté via `ObjectProvider`.
  - Clé absente/vide ⇒ supplier renvoyant `null` ⇒ provider indisponible : **le démarrage
    n'échoue jamais** faute de clé.

## Pourquoi un module dédié ?

- Les 3 services IA devaient tous charger : les starters OpenAI + Ollama, la configuration
  des 3 providers, le `ChatProviderResolver` et ses bases (anciennement dupliqué dans
  `chat-service`).
- `common` ne dépend **pas** de `ai-common` (dépendance à sens unique `ai-common` → `common`,
  aucune dépendance circulaire).
- Le circuit breaker (`spring-cloud-starter-circuitbreaker-resilience4j`) est fourni en
  transitif pour que chaque service puisse annoter ses appels LLM.

## Dépendances principales

| Artifact | Rôle |
|---|---|
| `common` | Enveloppe API, `ErrorCode`, `ApiException` |
| `spring-ai-starter-model-openai` | Provider Groq + base du client Gemini |
| `spring-ai-starter-model-ollama` | Provider Ollama (chat + embedding) |
| `spring-cloud-starter-circuitbreaker-resilience4j` | `@CircuitBreaker` utilisable par les services |

## Configuration requise chez le consommateur

`application.yml` du service consommateur (valeurs par défaut) :

```yaml
spring:
  # Auto-configs OpenAI du starter exclues (voir LlmProviderAutoConfiguration) ;
  # pour les services avec Qdrant, l'exclusion d'OpenAiEmbeddingAutoConfiguration
  # est aussi requise : le vector store n'accepte qu'un seul EmbeddingModel.
  autoconfigure:
    exclude:
      - org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
      - org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
  ai:
    retry:
      # Échec rapide : la résilience LLM relève du circuit breaker llm-* du service,
      # pas de retries HTTP transparents (qui dépassent le timeout de la gateway).
      max-attempts: 1
    openai:
      base-url: ${GROQ_BASE_URL:https://api.groq.com/openai}
      api-key: ${GROQ_API_KEY:}
      chat:
        options:
          model: ${GROQ_MODEL:llama-3.3-70b-versatile}
    gemini:
      base-url: ${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta/openai}
      api-key: ${GEMINI_API_KEY:}
      chat:
        options:
          model: ${GEMINI_MODEL:gemini-2.5-flash}
    ollama:
      base-url: ${OLLAMA_URL:http://localhost:11434}
      chat:
        options:
          model: ${OLLAMA_MODEL:qwen2.5:3b}
      embedding:
        options:
          model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}
chat:
  llm:
    active-provider: ${ACTIVE_LLM_PROVIDER:ollama}
```

> ⚠️ Les valeurs `api-key` vides sont acceptées au démarrage (utile pour Ollama) ; un appel à
> `ChatProviderResolver.current()` sur un provider non configuré lève
> `LLM_PROVIDER_UNAVAILABLE` (503) à l'exécution. Le modèle Groq par défaut existe à deux
> niveaux :
> - **Module ai-common** (`@Value` dans `LlmProviderAutoConfiguration`) : `llama-3.3-70b-versatile`
> - **Application** (`application.yml`) : `openai/gpt-oss-120b` — valeur constatée en août 2026.
>   C'est la valeur d'`application.yml` qui prévaut au runtime car elle est résolue en premier.
> Le catalogue Groq évolue, vérifier `GET /openai/v1/models` avec sa clé si `model_not_found`.

## Fichiers

```
ai-common/src/main/java/mg/esmia/miage/aicommon/
├── ChatProviderResolver.java
└── config/
    └── LlmProviderAutoConfiguration.java
ai-common/src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```