"""docling-worker — service d'extraction de documents (FastAPI).

Conteneur spawné à la demande par `ingestion-service` (jamais un service permanent de
docker-compose.yml). Pipeline (spec v2) :
  1. MarkItDown : PDF/DOCX/PPTX/XLSX/XLS/CSV/HTML/EPUB/TXT/MD -> Markdown structuré
     (CPU-only, sans modèle ML).
  2. Vision Gemini : transcription des pages si le document est scanné, sinon légende des
     images embarquées + placeholders ``{{IMAGE:img_001}}`` (les images sont renvoyées en
     base64, c'est ingestion-service qui les uploade dans MinIO).

Endpoints :
  - GET  /health     -> {"status": "ok"}
  - POST /v1/convert -> {"markdown", "method", "pages_processed", "images", "warnings"}

``method`` : ``markitdown`` (document textuel) ou ``markitdown_with_page_transcription``
(document scanné, pages transcrites par Gemini).
"""
import logging

from fastapi import FastAPI, File, HTTPException, UploadFile

from app.markitdown_converter import MarkItDownConverter

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="docling-worker", version="0.2.0")

# Le client Gemini est lazy-loadé (première image/page transcrite qui en a besoin).
converter = MarkItDownConverter()


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/v1/convert")
async def convert(file: UploadFile = File(...)) -> dict:
    if file is None or not file.filename:
        raise HTTPException(status_code=400, detail="Champ multipart 'file' manquant ou sans nom")
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Fichier vide")

    try:
        result = converter.convert(content, file.filename)
        result.setdefault("markdown", "")
        result.setdefault("method", "unknown")
        result.setdefault("pages_processed", 0)
        result.setdefault("images", [])
        result.setdefault("warnings", [])
        return result
    except Exception as e:  # noqa: BLE001 - erreur inattendue : réponse d'erreur explicite
        logger.exception("Conversion échouée pour %s", file.filename)
        raise HTTPException(status_code=422, detail=f"Conversion impossible : {e}") from e
