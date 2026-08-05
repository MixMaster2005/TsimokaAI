"""docling-worker — service d'extraction de documents (FastAPI).

Conteneur spawné à la demande par `ingestion-service` (jamais un service permanent de
docker-compose.yml). Deux étages :
  1. MarkItDown : PDF/DOCX/PPTX/XLSX -> Markdown structuré (CPU-only, sans modèle ML).
  2. OCR fallback (placeholder) : déclenché quand le ratio caractères/pages est trop bas.

Endpoints :
  - GET  /health     -> {"status": "ok"}
  - POST /v1/convert -> {"markdown", "method", "pages_processed", "warnings"}
"""
import logging

from fastapi import FastAPI, File, HTTPException, UploadFile

from app.markitdown_converter import MarkItDownConverter

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="docling-worker", version="0.1.0")

# Étage 1 (l'étage 2, lui, est lazy-loaded dans MarkItDownConverter._get_ocr()).
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
        result.setdefault("warnings", [])
        return result
    except Exception as e:  # noqa: BLE001 - erreur inattendue : réponse d'erreur explicite
        logger.exception("Conversion échouée pour %s", file.filename)
        raise HTTPException(status_code=422, detail=f"Conversion impossible : {e}") from e
