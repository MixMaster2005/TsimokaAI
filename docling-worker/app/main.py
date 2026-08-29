"""docling-worker — service d'extraction de documents (FastAPI).

Conteneur spawné à la demande par `ingestion-service` (jamais un service permanent de
docker-compose.yml). Pipeline (spec v3) :
  1. PDF : PyMuPDF layout extraction → RawPageModel → StructureAnalyzer → Canonical AST
     → Markdown dérivé (CPU-only, sans LLM).
  2. DOCX/PPTX/XLSX/etc : MarkItDown → Markdown (inchangé).
  3. Vision Gemini : transcription des pages si le document est scanné, sinon légende des
     images embarquées + placeholders ``{{IMAGE:img_001}}``.

Endpoints :
  - GET  /health     -> {"status": "ok"}
  - POST /v1/convert -> {"document", "markdown", "method", "pages_processed", "images", "warnings"}

``method`` : ``pymupdf_layout`` (PDF via le nouveau pipeline), ``markitdown`` (autres formats),
ou ``markitdown_with_page_transcription`` (document scanné).
"""
import logging

from fastapi import FastAPI, File, HTTPException, UploadFile

from app.markitdown_converter import DocumentConverter

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="docling-worker", version="0.3.0")

# Le client Gemini est lazy-loadé (première image/page transcrite qui en a besoin).
converter = DocumentConverter()


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
        result.setdefault("document", None)
        result.setdefault("markdown", "")
        result.setdefault("method", "unknown")
        result.setdefault("pages_processed", 0)
        result.setdefault("images", [])
        result.setdefault("warnings", [])
        return result
    except Exception as e:  # noqa: BLE001 - erreur inattendue : réponse d'erreur explicite
        logger.exception("Conversion échouée pour %s", file.filename)
        raise HTTPException(status_code=422, detail=f"Conversion impossible : {e}") from e
