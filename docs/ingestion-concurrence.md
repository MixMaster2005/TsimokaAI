# Concurrence d'ingestion : état actuel et pistes de gestion

Ce document analyse la gestion de la concurrence dans `ingestion-service` (pipeline
`@Async` + conteneur `docling-worker` spawné à la demande). Il sert de base à la section
« discussion » du mémoire : il décrit le comportement **actuel**, pourquoi il est risqué,
et les **pistes de correction** envisagées — avec les arguments valables en local comme en
serveur.

## 1. Comportement actuel

- `IngestionPipelineService.processAsync` est annoté `@Async` (`@EnableAsync` dans
  `IngestionServiceApplication`), **sans `TaskExecutor` personnalisé** : Spring utilise
  donc le `SimpleAsyncTaskExecutor` par défaut.
- Conséquence : **un thread par upload**, sans pool, sans file d'attente, sans borne.
- Chaque conversion spawné son propre conteneur `docling-worker-<uuid>` via docker-java
  (`DockerWorkerClient`), sur le réseau `apa-net`.
- Un upload répond immédiatement (`201`), le traitement est asynchrone ; l'état du document
  (`PENDING → PROCESSING → READY | FAILED`) sert de point de synchronisation.

Résultat : `N` uploads simultanés = `N` threads + `N` conteneurs Docker + `N × M` appels
Gemini (vision). Rien ne borne ce parallélisme aujourd'hui.

## 2. Pourquoi c'est un problème : trois ressources en contention

Ce n'est pas le nombre de threads qui est le goulot, mais trois ressources partagées :

1. **L'hôte (CPU / RAM / Docker)** — la machine cible du mémoire est un Intel i5-6200U,
   8 Go de RAM, sans GPU. Chaque conteneur `docling-worker` consomme ~250-400 Mo (Python,
   PyMuPDF, markitdown) et son démarrage coûte ~1-2 s.
2. **L'API Gemini** — les appels vision sont facturés et limités par quota (RPM, surtout en
   free tier). Un document textuel avec figures = jusqu'à 30 appels `caption_figure` ; un
   document scanné = 1 appel par page.
3. **Les threads** — en pratique le facteur le moins critique.

Borner uniquement un pool de threads serait donc une fausse solution : 2 threads peuvent
chacun spawner un conteneur *et* lancer 30 appels Gemini en parallèle. La gestion doit être
**multi-niveaux**.

## 3. Risque concret (chiffrage)

5 uploads simultanés sur la machine cible :

- 5 threads + 5 conteneurs ≈ +1,5-2 Go de RAM → risque d'OOM / de thrashing sous 8 Go.
- 5 × 30 = 150 appels Gemini en rafale → dépassement quasi certain du quota RPM (free tier),
  échecs 429/5xx en cascade, retards de traitement.
- Le `SimpleAsyncTaskExecutor` ne fait aucun backpressure : les uploads s'accumulent sans
  signalement à l'utilisateur.

## 4. Pistes de correction (discussion mémoire)

Trois verrous + une politique de retry, complémentaires :

1. **Pool asynchrone borné** — `ThreadPoolTaskExecutor` (ex. `core = max = 2` en local,
   file bornée ~10). L'upload reste en `201` ; tant qu'aucun slot n'est libre, le document
   demeure `PENDING` en base — c'est un état **observable**, pas une erreur. Remplacer
   explicitement le `SimpleAsyncTaskExecutor` par défaut.
2. **Sémaphore de conteneurs** (le vrai verrou) — `acquire()` avant
   `dockerWorkerClient.convert(...)`, `release()` en `finally`. `permits = 1-2` en local.
   C'est lui qui plafonne les spawn Docker simultanés et lisse la charge sur l'hôte.
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

- **En local** (8 Go, 4 cœurs, chaque service limité à ~450 Mo via `mem_limit`) : la limite
  est la RAM + le quota Gemini. Un sémaphore **in-JVM** (par instance de `ingestion-service`)
  suffit, avec 1-2 workers parallèles maximum.
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

- Choix du `TaskExecutor` et de ses paramètres (à ajuster empiriquement).
- Localisation du sémaphore : service unique vs distribué (Redisson).
- Gestion du quota Gemini (throttling côté worker vs backpressure côté ingestion).
- Impact du coût de spawn (pool chaud) si la latence devient un critère.
