# Concurrence d'ingestion : état actuel et pistes de gestion

Ce document analyse la gestion de la concurrence dans `ingestion-service` (pipeline
`@Async` + conteneur `docling-worker` spawné à la demande). Il sert de base à la section
« discussion » du mémoire : il décrit le comportement **actuel**, pourquoi il est risqué,
et les **pistes de correction** envisagées — avec les arguments valables en local comme en
serveur.

## 1. Comportement actuel

- `IngestionPipelineService.processAsync` est annoté `@Async` (`@EnableAsync` dans
  `IngestionServiceApplication`), avec un **`ThreadPoolTaskExecutor` borné** (core=2,
  max=4, queue=10, CallerRunsPolicy) configuré dans `AsyncConfig`.
- Un **sémaphore Docker** (`java.util.concurrent.Semaphore`, permits configurable via
  `DOCLING_WORKER_MAX_CONTAINERS`, défaut=4) dans `DockerWorkerClient` plafonne les
  conteneurs `docling-worker` simultanés.
- Chaque conversion spawné son propre conteneur `docling-worker-<uuid>` via docker-java
  (`DockerWorkerClient`), sur le réseau `apa-net`.
- Un upload répond immédiatement (`201`), le traitement est asynchrone ; l'état du document
  (`PENDING → PROCESSING → READY | FAILED`) sert de point de synchronisation.

Résultat : les uploads excédentaires attendent dans la file du pool de threads. Les threads
actifs attendent sur `semaphore.acquire()` si N conteneurs sont déjà actifs. La RAM/CPU
hôte ne sont jamais saturés par les containers.

## 2. Pourquoi c'est un problème : trois ressources en contention

Ce n'est pas le nombre de threads qui est le goulot, mais trois ressources partagées :

1. **L'hôte (CPU / RAM / Docker)** — la machine cible du mémoire est un Intel i5-6200U,
   16 Go de RAM, 4 cœurs physiques, sans GPU. Chaque conteneur `docling-worker` consomme
   ~250-400 Mo (Python, PyMuPDF, markitdown) et son démarrage coûte ~1-2 s.
2. **L'API Gemini** — les appels vision sont facturés et limités par quota (RPM, surtout en
   free tier). Un document textuel avec figures = jusqu'à 30 appels `caption_figure` ; un
   document scanné = 1 appel par page.
3. **Les threads** — en pratique le facteur le moins critique.

Borner uniquement un pool de threads serait donc une fausse solution : 2 threads peuvent
chacun spawner un conteneur *et* lancer 30 appels Gemini en parallèle. La gestion doit être
**multi-niveaux**.

## 3. Risque concret (chiffrage)

5 uploads simultanés sur la machine cible (sans sémaphore) :

- 5 threads + 5 conteneurs ≈ +1,5-2 Go de RAM → risque d'OOM / de thrashing sous 16 Go.
- 5 × 30 = 150 appels Gemini en rafale → dépassement quasi certain du quota RPM (free tier),
  échecs 429/5xx en cascade, retards de traitement.
- Sans backpressure : les uploads s'accumulent sans signalement à l'utilisateur.

**Avec le sémaphore (configuré à 4)** : les 5e et suivants attendent dans la file du pool.
Maximum 4 conteneurs simultanés = ~1.6 Go RAM pour les workers, large marge sur 16 Go.

## 4. Pistes de correction — état d'implémentation

### IMPLÉMENTÉ

1. **Pool asynchrone borné** — `ThreadPoolTaskExecutor` (core=2, max=4, queue=10,
   CallerRunsPolicy) dans `AsyncConfig.java`. L'upload reste en `201` ; tant qu'aucun slot
   n'est libre, le document demeure `PENDING` en base — c'est un état **observable**, pas
   une erreur.
2. **Sémaphore de conteneurs** (le vrai verrou) — `Semaphore(maxContainers, true)` dans
   `DockerWorkerClient.java`. `acquire()` avant `startContainer()`, `release()` en
   `finally` (après `stopAndRemoveContainer()`). `permits = 4` par défaut, configurable
   via `DOCLING_WORKER_MAX_CONTAINERS`. C'est lui qui plafonne les spawn Docker
   simultanés et lisse la charge sur l'hôte.

### EN ATTENTE

3. **Idempotence par document** — transition atomique `PENDING → PROCESSING` (lock Redis
   `SETNX` keyé par `documentId`, ou `SELECT ... FOR UPDATE`) pour empêcher un double
   traitement (double-clic, retry client).
4. **Retry ciblé avec backoff** — sur les échecs transitoires uniquement (spawn Docker,
   Gemini 429/5xx), hors transaction, avec un nombre d'essais max puis `FAILED`.

**Backpressure** : file pleine → ne pas rejeter l'upload ; laisser `PENDING` et faire
reprendre les documents stagnants par un *sweeper* planifié. Alternative simple pour le
mémoire : `CallerRunsPolicy` (le thread de l'upload exécute la conversion, effet naturel
de ralentissement).

## 5. Local vs serveur

- **En local** (16 Go, 4 cœurs, chaque service limité à ~450 Mo via `mem_limit`) : la limite
  est la RAM + le quota Gemini. Le sémaphore **in-JVM** (par instance de `ingestion-service`)
  est configuré à 4 workers parallèles maximum (~1.6 Go RAM pour les workers).
- **En serveur** : mêmes principes, mais si `ingestion-service` est déployé en **plusieurs
  réplicas**, un sémaphore JVM n'est plus *global*. Il faut alors :
  - un **sémaphore distribué Redis** (Redisson) ou un verrou par document, pour borner la
    concurrence à l'échelle du cluster ;
  - une **file Redis/DB** (au lieu d'une file in-JVM) pour que les conversions en attente
    survivent à un redémarrage ;
  - ne jamais spawner plus de conteneurs `docling-worker` que de cœurs disponibles.
- Option avancée (à garder hors périmètre du mémoire) : **pool chaud** de workers idle pour
  économiser le ~1-2 s de spawn à chaque conversion.

## 6. Points ouverts

- Ajustement empirique du `max-containers` et du `TaskExecutor` selon la charge réelle.
- Localisation du sémaphore : service unique vs distribué (Redisson).
- Gestion du quota Gemini (throttling côté worker vs backpressure côté ingestion).
- Impact du coût de spawn (pool chaud) si la latence devient un critère.
- **Job queue** (Redis/RabbitMQ) pour remplacer la file in-JVM du ThreadPoolTaskExecutor —
  survive aux redémarrages et permet un déploiement multi-réplicas.
