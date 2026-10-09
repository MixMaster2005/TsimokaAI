## 2.2.2.3 Résultats de vérification de l’hypothèse 3 : « Une architecture pensée pour l'évolutivité permet à la plateforme d'accompagner un nombre croissant d'utilisateurs sans dégradation du service »

La vérification de cette hypothèse a porté sur l'organisation de l'architecture logicielle et sur le comportement de la plateforme lors de sollicitations simultanées. Les résultats présentés concernent la répartition des responsabilités entre les services, les possibilités d'évolution relevées dans la configuration, l'organisation des traitements concurrents ainsi que les mesures obtenues lors des différents essais.

### 1. Organisation des services et des composants

L'architecture déployée comprend une gateway et sept microservices : `api-gateway`, `user-service`, `space-service`, `ingestion-service`, `chat-service`, `fiche-service`, `analytics-service` et `gamification-service`. Chaque service est associé à un domaine fonctionnel spécifique.

Le tableau suivant présente les services et les principaux composants utilisés dans leur fonctionnement.

| Service / composant    | Port / accès   | Responsabilité observée                                                                   |
| ---------------------- | -------------- | ----------------------------------------------------------------------------------------- |
| Frontend               | :3000          | Interface React/Vite                                                                      |
| `api-gateway`          | :8080          | Vérification JWT, enrichissement des requêtes, routage vers les services et rate limiting |
| `user-service`         | :8081          | Inscription, connexion, gestion des comptes et profils                                    |
| `space-service`        | :8082          | Gestion des espaces, groupes, invitations et persona pédagogique                          |
| `ingestion-service`    | :8083          | Dépôt, extraction, découpage, embeddings et indexation des documents                      |
| `chat-service`         | :8084          | Conversations et traitement RAG                                                           |
| `fiche-service`        | :8085          | Fiches, quiz, partage, annotations et validation                                          |
| `analytics-service`    | :8086          | Tableaux de bord, compteurs et recommandations                                            |
| `gamification-service` | :8087          | Objectifs, badges et rappels                                                              |
| PostgreSQL             | interne :5432  | Stockage des données des sept services dans sept bases distinctes                         |
| Redis                  | :6379          | Pub/Sub, rate limiting, déduplication et cache                                            |
| Qdrant                 | :6333 / :6334  | Stockage et recherche des passages vectorisés                                             |
| MinIO                  | :9000 / :9001  | Stockage des documents et figures extraites                                               |
| Ollama                 | interne :11434 | Génération des embeddings et fournisseur local de modèles                                 |

Les services métier utilisent chacun une base PostgreSQL distincte au niveau applicatif. Les événements entre services sont diffusés par Redis Pub/Sub. Les quatre services utilisant la recherche documentaire (`space-service`, `ingestion-service`, `chat-service` et `fiche-service`) accèdent à la collection `chunks` de Qdrant. Le stockage des documents est assuré par MinIO depuis `ingestion-service`.

Pour les traitements d'ingestion, `docling-worker` est lancé dans un conteneur temporaire. Le conteneur est créé pour une conversion, puis arrêté et supprimé à la fin du traitement.

### 2. Diagramme d'architecture global

**Figure X : Diagramme d'architecture global de la plateforme**

Le diagramme présente le frontend relié à la gateway, puis la gateway reliée aux sept services. Il représente également les bases PostgreSQL associées aux services, ainsi que Redis, Qdrant, MinIO, Ollama et `docling-worker`.

Les relations synchrones et les notifications par Redis Pub/Sub y sont représentées séparément. La collection `chunks` de Qdrant est commune aux services qui utilisent les passages vectorisés. Le diagramme comporte également une note indiquant qu'une séparation des instances PostgreSQL peut être réalisée par modification de la configuration `DB_URL`.

### 3. Possibilités d'évolution relevées

L'analyse de la configuration fait apparaître deux dimensions : l'augmentation des ressources d'une instance et la multiplication des instances.

#### 3.1 Augmentation des ressources d'une instance

Les fichiers de configuration relèvent les paramètres suivants :

| Élément                                               | Valeur relevée                    |
| ----------------------------------------------------- | --------------------------------- |
| JVM des services                                      | `-XX:MaxRAMPercentage=75`         |
| Mémoire gateway, user, space, chat et fiche           | 400 MiB par conteneur             |
| Mémoire ingestion                                     | 450 MiB                           |
| Mémoire analytics, gamification, PostgreSQL et Qdrant | 350 MiB par conteneur             |
| Mémoire Redis                                         | 100 MiB                           |
| Mémoire MinIO                                         | 200 MiB                           |
| Mémoire Ollama                                        | 2500 MiB                          |
| Pool Hikari                                           | 5 connexions maximum par instance |
| Taille maximale d'un fichier                          | 20 Mo                             |
| Taille maximale d'une requête multipart               | 20 Mo                             |
| Délai de connexion gateway                            | 5 s                               |
| Délai de réponse gateway                              | 30 min                            |

Les plafonds mémoire relevés dans `docker-compose.yml` concernent les différents conteneurs déployés. Les limites effectives relevées avec `docker inspect` sont de 471 859 200 octets pour `ingestion-service` et de 419 430 400 octets pour la gateway.

#### 3.2 Multiplication des instances

Les services applicatifs ne conservent pas de session utilisateur en mémoire locale dans les éléments observés. Les informations de conversation sont stockées dans la base de données et les mécanismes de déduplication utilisent Redis.

La configuration relevée indique par ailleurs que les limites de traitement de l'ingestion sont définies localement dans chaque instance :

* `ThreadPoolTaskExecutor` : `core=2`, `max=4`, `queue=10` ;
* `Semaphore` : 4 permis pour les conteneurs `docling-worker` ;
* `CallerRunsPolicy` lorsque la file du pool est pleine.

La file du pool et le sémaphore sont gérés dans la mémoire de l'instance et ne sont pas partagés entre plusieurs réplicas.

**Figure X : Leviers de scalabilité verticale et horizontale de la plateforme**

Le schéma distingue les services applicatifs, les composants partagés et les mécanismes locaux de traitement de l'ingestion.

### 4. Traitement des sollicitations simultanées et de l'ingestion

**Figure X : Diagramme de séquence des sollicitations simultanées via la gateway**

Le diagramme représente plusieurs clients envoyant simultanément des requêtes à la gateway. Les requêtes passent par la vérification JWT, le rate limiting et le routage vers les services concernés. Les services accèdent ensuite à leurs bases de données et, selon l'opération, à Redis, Qdrant ou au fournisseur LLM.

**Figure X : Diagramme d'activité de l'ingestion différée et concurrente**

Le dépôt d'un document est suivi d'une réponse HTTP `201` avec l'état `PENDING`. Le traitement comprend ensuite l'extraction, le chunking, la génération des embeddings, l'indexation dans Qdrant et la mise à jour du statut du document.

Le pool d'exécution est configuré avec deux threads de base, quatre threads au maximum et une file de dix tâches. Le sémaphore limite à quatre le nombre de conteneurs `docling-worker` pouvant être utilisés simultanément par une instance.

### 5. Résultats des essais de sollicitation

Les essais ont été exécutés le 6 octobre 2026 sur un hôte disposant de 4 CPU et de 15 Gio de mémoire. Les huit services du backend ont été démarrés avant les mesures. Le test E2E de référence a produit huit étapes réussies.

#### 5.1 Demandes simultanées en lecture

Des requêtes `GET /api/v1/spaces` ont été envoyées simultanément via la gateway.

| Nombre de requêtes simultanées | Réussites | Temps minimal | Temps médian | Temps maximal | Rejets 429/5xx |
| ------------------------------ | --------- | ------------- | ------------ | ------------- | -------------- |
| 1                              | 1/1       | 35 ms         | 35 ms        | 35 ms         | 0              |
| 10                             | 10/10     | 87 ms         | 161 ms       | 205 ms        | 0              |
| 25                             | 25/25     | 124 ms        | 348 ms       | 423 ms        | 0              |

Les 1, 10 et 25 requêtes exécutées ont obtenu une réponse HTTP 200. Aucun code 429 ni 5xx n'a été relevé pendant ces essais.

#### 5.2 Demandes simultanées en écriture

Des créations d'espaces ont été exécutées simultanément. Chaque opération comprenait une génération du persona pédagogique par le fournisseur Groq.

| Nombre de créations simultanées | Réussites | Temps minimal | Temps maximal |
| ------------------------------- | --------- | ------------- | ------------- |
| 5                               | 5/5       | 1 419 ms      | 1 812 ms      |
| 10                              | 10/10     | 1 189 ms      | 3 687 ms      |

Les cinq créations ont obtenu une réponse HTTP 201, de même que les dix créations du second essai.

#### 5.3 Ingestions concurrentes

Six fichiers texte de 4 413 octets ont été déposés simultanément.

Les six requêtes ont obtenu une réponse HTTP 201 dans un délai compris entre 690 et 770 ms. Les documents ont d'abord été enregistrés avec le statut `PENDING`.

| Temps après le dépôt | PENDING | READY | FAILED | Conteneurs `docling-worker` simultanés |
| -------------------- | ------: | ----: | -----: | -------------------------------------: |
| +15 s                |       6 |     0 |      0 |                                      2 |
| +30 s                |       5 |     1 |      0 |                                      1 |
| +45 s                |       4 |     2 |      0 |                                      1 |
| +60 s                |       2 |     4 |      0 |                                      1 |
| +75 s                |       0 |     6 |      0 |                                      0 |
| +90 s                |       0 |     6 |      0 |                                      0 |

Les six documents ont atteint l'état `READY` au plus tard à +75 s. Aucun document n'a atteint l'état `FAILED`. Les documents obtenus présentent un `chunkCount` égal à 1 et un `failureReason` nul.

Les logs indiquent six créations de conteneurs `docling-worker-<uuid>` : deux au début du traitement, puis les quatre autres successivement. La collection Qdrant contenait 104 points au moment du relevé.

#### 5.4 Mémoire observée pendant l'ingestion

| Conteneur                      | Mémoire utilisée |   Plafond |
| ------------------------------ | ---------------: | --------: |
| `tsimoka-ingestion-service`    |          403 MiB |   450 MiB |
| `tsimoka-space-service`        |          396 MiB |   400 MiB |
| `tsimoka-chat-service`         |          387 MiB |   400 MiB |
| `tsimoka-fiche-service`        |          365 MiB |   400 MiB |
| `tsimoka-gamification-service` |          265 MiB |   350 MiB |
| `tsimoka-analytics-service`    |          263 MiB |   350 MiB |
| `tsimoka-user-service`         |          243 MiB |   400 MiB |
| `tsimoka-api-gateway`          |          216 MiB |   400 MiB |
| `tsimoka-qdrant`               |          208 MiB |   350 MiB |
| `tsimoka-minio`                |          159 MiB |   200 MiB |
| `tsimoka-postgres`             |          130 MiB |   350 MiB |
| `tsimoka-redis`                |           14 MiB |   100 MiB |
| `tsimoka-ollama`               |          658 MiB | 2 441 MiB |

Le relevé a été effectué avec `docker stats --no-stream` pendant la fenêtre de traitement des six documents.

### 6. Synthèse des observations

Les résultats recueillis décrivent une architecture composée de sept microservices derrière une gateway, avec des bases PostgreSQL distinctes au niveau applicatif, Redis pour les événements et mécanismes associés, Qdrant pour les passages vectorisés et MinIO pour les documents.

Les paramètres de configuration relevés comprennent des limites mémoire par conteneur, un pool Hikari de cinq connexions par instance, ainsi qu'un pool de traitement de l'ingestion configuré avec deux threads de base, quatre threads maximum et une file de dix tâches.

Lors des essais, 25 requêtes de lecture simultanées ont toutes obtenu une réponse HTTP 200. Les cinq puis dix créations d'espaces simultanées ont toutes obtenu une réponse HTTP 201. Les six dépôts de documents simultanés ont également obtenu une réponse HTTP 201 et les six documents ont atteint l'état `READY` dans les 75 secondes suivant leur dépôt.

Aucun code 429 ou 5xx n'a été relevé lors des essais de lecture. Aucun document n'a atteint l'état `FAILED` lors de l'essai d'ingestion. Le nombre maximal de conteneurs `docling-worker` observé simultanément était de deux. Les relevés de mémoire effectués pendant cette séquence sont restés inférieurs aux plafonds configurés pour les conteneurs.
