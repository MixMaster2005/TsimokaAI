# docling-worker — extraction de documents (conteneur à la demande)

> **Statut :** 🟡 Étage 1 (MarkItDown) fonctionnel — étage 2 (OCR) en **placeholder**

Service Python **FastAPI** qui extrait le texte des documents de cours (PDF, DOCX, PPTX,
XLSX) en **Markdown structuré**. Il n'est **pas** un service permanent : `ingestion-service`
le **spawné à la demande** via l'API Docker (`docker-java`), l'appelle, puis arrête et
supprime le conteneur — même en cas d'erreur.

## Rôle

1. Recevoir un fichier en `multipart/form-data`.
2. **Étage 1 — MarkItDown** (`microsoft/markitdown`) : conversion en Markdown (titres,
   tableaux, listes). CPU-only, rapide, aucun modèle ML à charger.
3. **Décision de fallback** : calcul du ratio `chars_extraits / pages` (pages PDF comptées
   via `pypdf`, 1 page par défaut pour les formats non paginés). Si le ratio est sous le
   seuil `DOCLING_MIN_CHARS_PER_PAGE` (défaut **40**, à ajuster empiriquement) →
   document trop « image », bascule sur l'**étage 2**.
4. **Étage 2 — OCR** (*placeholder*) : modèle candidat `PaddlePaddle/PaddleOCR-VL-0.9B`
   (lazy-loading en singleton) ou `DeepSeek-OCR-2` quantifié GGUF via `llama.cpp`
   (`SandLogicTechnologies/DeepSeek-OCR-2-GGUF`) si perf CPU insuffisante. Pipeline attendu :
   rendu PDF→images par page (`pypdfium2`, ~150-200 DPI) → OCR par page → concaténation avec
   `\n\n---\n\n`. **L'API d'inférence exacte (transformers vs SDK dédié) est à confirmer**
   dans la doc HuggingFace du modèle avant implémentation.

## Endpoints

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/health` | `{"status": "ok"}` — sondé par `DockerWorkerClient` au démarrage du conteneur |
| `POST` | `/v1/convert` | `multipart`, champ `file` → `{"markdown", "method", "pages_processed", "warnings"}` |

Réponse type :

```json
{ "markdown": "# Titre\n\nTexte…", "method": "markitdown", "pages_processed": 12, "warnings": [] }
```

- `method` : `markitdown` ou `ocr_fallback`.
- `warnings` : non bloquant (ex. ratio faible, échec d'un étage).

## Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `DOCLING_MIN_CHARS_PER_PAGE` | `40` | Seuil ratio caractères/page déclenchant l'étage 2 |

## Lancer / tester en local

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090
curl -F "file=@exemple.pdf" http://localhost:8090/v1/convert
curl http://localhost:8090/health
```

## Construire l'image (une fois, à la racine du repo)

```bash
docker build -t docling-worker:latest ./docling-worker
```

Le conteneur est ensuite spawné automatiquement par `ingestion-service` (réseau `apa-net`,
nom `docling-worker-<uuid>`, port 8090) lors de chaque upload de document.

## Non implémenté (à combler pour le mémoire)

1. **Étage 2 OCR** : chargement réel du modèle + inférence par page + concaténation.
2. Ajustement empirique du seuil de fallback (`DOCLING_MIN_CHARS_PER_PAGE`).
