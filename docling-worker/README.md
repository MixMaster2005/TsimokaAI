# docling-worker — extraction de documents (conteneur à la demande)

> **Statut :** 🟢 Spec v2 — conversion MarkItDown + vision **Gemini** (figures & documents scannés)

Service Python **FastAPI** qui extrait le texte des documents de cours (PDF, DOCX, PPTX,
XLSX) en **Markdown structuré**. Il n'est **pas** un service permanent : `ingestion-service`
le **spawné à la demande** via l'API Docker (`docker-java`), l'appelle, puis arrête et
supprime le conteneur — même en cas d'erreur.

## Rôle (spec v2)

1. Recevoir un fichier en `multipart/form-data`.
2. **Étage 1 — MarkItDown** (`microsoft/markitdown`) : conversion en Markdown (titres,
   tableaux, listes). CPU-only, rapide, aucun modèle ML à charger.
3. **Extraction des images** embarquées (spec v2) : PyMuPDF pour le PDF, python-docx pour
   le DOCX, python-pptx pour le PPTX. Les images trop petites (logos, icônes) sont ignorées.
4. **Vision Gemini** (remplace l'OCR local de la spec v1 — plus aucun modèle dans le
   conteneur) :
   - document textuel → légende courte de chaque figure (`caption_figure`) ;
   - document scanné (ratio `chars_extraits / pages < DOCLING_MIN_CHARS_PER_PAGE`, défaut
     **40**) → rendu des pages en image (PyMuPDF, ~150 DPI) + transcription Gemini page par
     page (`transcribe_full_page`).
5. **Réponse enrichie** : les images sont renvoyées **en base64** avec des placeholders
   `{{IMAGE:img_001}}` insérés dans le Markdown (à la position des marqueurs MarkItDown,
   sinon en fin de document pour le PDF). `ingestion-service` uploade ensuite les images
   dans **MinIO** et remplace chaque placeholder par `![caption](url)` + une ligne
   `> **Description :** caption`.

## Endpoints

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/health` | `{"status": "ok"}` — sondé par `DockerWorkerClient` au démarrage du conteneur |
| `POST` | `/v1/convert` | `multipart`, champ `file` → `{"markdown", "method", "pages_processed", "images", "warnings"}` |

Réponse type (document textuel avec une figure) :

```json
{
  "markdown": "# Rapport\n\nTexte…\n\n{{IMAGE:img_001}}\n\nSuite…",
  "method": "markitdown",
  "pages_processed": 12,
  "images": [
    {
      "placeholder_id": "img_001",
      "content_type": "image/png",
      "data_base64": "iVBORw0KGgo…",
      "caption": "Schéma d'un graphe orienté"
    }
  ],
  "warnings": []
}
```

- `method` : `markitdown` (document textuel) ou `markitdown_with_page_transcription`
  (document scanné, pages transcrites par Gemini).
- `images` : max **30** (plafond `DOCLING_MAX_EXTRACTED_IMAGES`) — au-delà, warning.
- `warnings` : non bloquant (ex. ratio faible, légende/transcription Gemini en échec —
  l'échec de Gemini n'empêche jamais la conversion).

## Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `GEMINI_API_KEY` | *(obligatoire pour fig/scan)* | Injectée par ingestion-service via docker-java (OpenAI-compatible) |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Modèle vision utilisé |
| `DOCLING_MIN_CHARS_PER_PAGE` | `40` | Seuil ratio caractères/page déclenchant la transcription |
| `DOCLING_MAX_EXTRACTED_IMAGES` | `30` | Plafond d'images légendées par document |
| `DOCLING_MIN_IMAGE_DIMENSION` | `64` | Pixels min (largeur ou hauteur) pour ignorer logos/icônes |
| `GEMINI_MAX_RETRIES` / `GEMINI_RETRY_BACKOFF` | `3` / `1.5` | Retry avec backoff sur les appels Gemini |

## Lancer / tester en local

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090
curl -F "file=@exemple.pdf" http://localhost:8090/v1/convert
curl http://localhost:8090/health
```

Tests unitaires (sans clé Gemini — appels mockés) :

```bash
python -m unittest tests.test_converter -v
```

## Construire l'image (une fois, à la racine du repo)

```bash
docker build -t docling-worker:latest ./docling-worker
```

Le conteneur est ensuite spawné automatiquement par `ingestion-service` (réseau `apa-net`,
nom `docling-worker-<uuid>`, port 8090, env `GEMINI_API_KEY`) lors de chaque upload.

## Non implémenté / points ouverts

1. **Modèle Gemini** : nom par défaut `gemini-2.5-flash` — à confirmer (facturation,
   quota). Surchargable via `GEMINI_MODEL`.
2. **Licence AGPL-3.0 de PyMuPDF** : si contraignante pour le mémoire, remplacer par
   `pdfplumber`/`pypdfium2` dans `image_extractor.py` (rendu pages + extraction images).
3. **Position des placeholders PDF** : le PDF n'ayant pas de marqueur d'image MarkItDown,
   les placeholders sont collés en fin de Markdown (le positionnement par bloc texte +
   bounding box reste une amélioration possible).
