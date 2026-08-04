# fiche-service

> **Statut :** 🟡 CRUD complet — génération Map-Reduce en **TODO**
> **Port :** `8085` · **Base :** `fiche_db` (PostgreSQL) · **LLM :** Ollama (configuré)

Fiches de révision, partage, annotations et validation enseignant. Le CRUD complet
(fiches, partage, annotations, validation) est **fonctionnel** ; la **génération du contenu**
par le pattern Map-Reduce est le **cœur IA à implémenter** (`FicheGenerationService`).

## Rôle

1. **Générer** une fiche de révision à partir d'une liste de documents (sources indexées par
   ingestion-service) → contenu JSON structuré (`content_json`).
2. **Partager** une fiche à un destinataire ou à un groupe.
3. **Annoter** une fiche (notes rattachées à une section).
4. **Valider** une fiche (réservé aux enseignants) : `EN_ATTENTE / VALIDEE / REJETEE`.

## Choix techniques

- **Contenu structuré en JSONB** : `content_json` (colonne PostgreSQL `jsonb`) porte une
  structure typée `sections[]` avec types `definition`, `key_points`, `example` (contrat de la
  « Base de projet »). Le front peut donc rendre la fiche de façon déterministe.
- **Traçabilité des sources** : `source_document_ids UUID[]` (références logiques vers
  ingestion-service).
- **Génération par pattern Map-Reduce** (conforme CDC §4.4) :
  - **MAP** : par document → résumé intermédiaire structuré (appel LLM one-shot).
  - **REDUCE** : fusion des résumés en une fiche unique cohérente.
- **Obsolescence automatique** : à chaque `DOCUMENT_READY` reçu pour un espace, toutes les
  fiches existantes de cet espace sont marquées `obsolete = true` (une fiche ne peut pas être
  à jour si un nouveau document a été ingéré après sa génération).
- **Validation = 1 fiche ↔ 1 validation** : `validation_fiche.fiche_id` est `UNIQUE`
  (une nouvelle validation écrase la précédente — upsert).
- **Partage orienté** : `groupeId` **OU** `destinataireId` (un seul des deux, validé métier).

## Génération de fiche (pattern Map-Reduce, cible)

```mermaid
flowchart LR
    subgraph Entrée
        REQ[POST /fiches/generate<br/>spaceId + documentIds]
    end
    REQ --> MAP
    subgraph MAP[Phase MAP - par document]
        M1[Charger les chunks du document<br/>via ingestion-service]
        M2[LLM one-shot<br/>résumé intermédiaire structuré]
    end
    MAP --> RED[Phase REDUCE<br/>LLM - fusion des résumés]
    RED --> JSON[sections: definition / key_points / example]
    JSON --> DB[(fiches.content_json)]
```

> ❗ **État actuel** : `FicheGenerationService.generateContentJson()` retourne un **placeholder
> statique** (structure vide, sections « TODO »). La génération réelle est à écrire.

## Cycle de vie d'une fiche

```mermaid
stateDiagram-v2
    [*] --> Générée : POST /fiches/generate
    Générée --> Obsolète : DOCUMENT_READY reçu (nouvelle ingestion)
    Générée --> En_validation : validation créée
    En_validation --> Validée : enseignant (statut VALIDEE)
    En_validation --> Rejetée : enseignant (statut REJETEE)
    Validée --> Obsolète : DOCUMENT_READY reçu
    Obsolète --> [*] : suppression
```

## Endpoints

Toutes les routes sont protégées par JWT.

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| POST | `/api/v1/fiches/generate` | connecté | Générer une fiche (spaceId + documentIds) |
| GET | `/api/v1/fiches?spaceId={id}` | connecté | Lister **mes** fiches d'un espace |
| GET | `/api/v1/fiches/{id}` | propriétaire/admin | Détail d'une fiche |
| DELETE | `/api/v1/fiches/{id}` | propriétaire/admin | Supprimer (cascade partages/annotations/validation) |
| POST | `/api/v1/fiches/{id}/share` | propriétaire | Partager à un groupe ou un destinataire |
| GET | `/api/v1/fiches/{id}/share` | connecté | Lister les partages |
| POST | `/api/v1/fiches/{id}/annotations` | connecté | Ajouter une annotation (sectionRef optionnelle) |
| GET | `/api/v1/fiches/{id}/annotations` | connecté | Lister les annotations |
| PUT | `/api/v1/fiches/{id}/validation` | enseignant (admin) | Valider/rejeter (statut + commentaire) |
| GET | `/api/v1/fiches/{id}/validation` | connecté | Lire la validation (défaut `EN_ATTENTE`) |

## Règles métier

- **Seul le propriétaire** (ou admin) peut lire/supprimer/partager une fiche.
- **Seul un enseignant** (`ADMIN`/`ENSEIGNANT` au sens `UserContext.isAdmin()`) peut valider
  → `403` sinon.
- **Partage** : fournir `groupeId` **ou** `destinataireId`, jamais les deux ni aucun → `400`.
- **Obsolescence** : toute nouvelle ingestion dans l'espace rend les fiches existantes obsolètes.
- **Validation unique** : revalider une fiche remplace la validation précédente.
- La suppression d'une fiche supprime en cascade partages, annotations et validation (FK).

## Événements

| Canal | Événement | Direction | Rôle |
|---|---|---|---|
| `fiche.events` | `FICHE_GENERATED` | publié | Progression étudiant (analytics) + suivi hebdo/badges (gamification) |
| `fiche.events` | `FICHE_VALIDATED` | publié | Progression (analytics) + badge validation (gamification) |
| `ingestion.events` | `DOCUMENT_READY` | consommé | Marquage obsolescence des fiches de l'espace |
| `space.events` | `SPACE_DELETED` | consommé | Purge des fiches de l'espace |
| `user.events` | `USER_DELETED` | consommé | Purge des fiches de l'utilisateur |

## Modèle de données

- `fiches` : `id`, `space_id` (logique), `user_id` (logique), `title`, `source_document_ids UUID[]`,
  `content_json JSONB`, `obsolete`, `generated_at`, `updated_at`.
- `partage_fiche` : `id`, `fiche_id (FK cascade)`, `groupe_id` (nullable), `destinataire_id` (nullable),
  `partage_par`, `shared_at`.
- `annotations` : `id`, `fiche_id (FK cascade)`, `auteur_id` (logique), `contenu`, `section_ref`, `created_at`.
- `validation_fiche` : `id`, `fiche_id (FK cascade, UNIQUE)`, `enseignant_id`, `statut`, `commentaire`, `validated_at`.

## Non implémenté (cœur IA du mémoire) — `FicheGenerationService`

- **MAP** : récupérer les chunks de chaque document (appel REST vers ingestion-service, ou
  lecture des métadonnées exposées) puis produire un **résumé intermédiaire structuré** par
  appel LLM one-shot.
- **REDUCE** : fusionner les résumés en une **fiche unique cohérente** respectant la structure
  `definition / key_points / example` et sérialiser en JSON pour `content_json`.
- Provider LLM : bascule par `ACTIVE_LLM_PROVIDER` (Ollama configuré ; Groq/Gemini à câbler).

## Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | LLM de génération (fallback local) |
| `OLLAMA_MODEL` | `qwen2.5:3b` | Modèle de génération |

## Lancer

```bash
docker compose up -d postgres redis
mvn -pl common,fiche-service -am spring-boot:run
# Swagger : http://localhost:8085/swagger-ui.html
```
