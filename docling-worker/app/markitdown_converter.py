"""Étage 1 — conversion MarkItDown (PDF/DOCX/PPTX/XLSX -> Markdown structuré).

Convertit le fichier en Markdown (titres, tableaux), puis décide, selon le ratio
``caractères extraits / nombre de pages``, si le document est assez textuel pour être
exploité tel quel ou s'il faut basculer sur l'étage 2 (OCR).

Règle de fallback : ``chars_extraits / pages < DOCLING_MIN_CHARS_PER_PAGE`` (défaut 40,
à ajuster empiriquement) -> déclencher l'étage 2.
"""
import io
import logging
import os
import threading

from markitdown import MarkItDown
from pypdf import PdfReader

logger = logging.getLogger(__name__)

MIN_CHARS_PER_PAGE = int(os.environ.get("DOCLING_MIN_CHARS_PER_PAGE", "40"))


def count_pages(content: bytes, filename: str) -> int:
    """Nombre de pages du document : comptage PDF via pypdf, 1 par défaut sinon."""
    if os.path.splitext(filename)[1].lower() != ".pdf":
        return 1
    try:
        reader = PdfReader(io.BytesIO(content))
        return max(1, len(reader.pages))
    except Exception as e:  # noqa: BLE001 - fichier PDF corrompu/illisible : valeur par défaut
        logger.warning("Impossible de compter les pages PDF de %s (%s) — 1 par défaut", filename, e)
        return 1


class MarkItDownConverter:
    """Étage 1 : conversion MarkItDown + décision de fallback vers l'OCR."""

    def __init__(self) -> None:
        self._md = MarkItDown()
        self._ocr = None
        self._lock = threading.Lock()

    def _get_ocr(self):
        """Étage 2, lazy-loaded : instancié seulement si un document déclenche le fallback."""
        if self._ocr is None:
            with self._lock:
                if self._ocr is None:
                    from app.ocr_fallback import OcrFallback

                    self._ocr = OcrFallback()
        return self._ocr

    def convert(self, content: bytes, filename: str) -> dict:
        warnings = []
        pages = count_pages(content, filename)

        markdown = ""
        try:
            extension = os.path.splitext(filename)[1]
            markdown = self._md.convert_stream(
                io.BytesIO(content), file_extension=extension
            ).text_content or ""
        except Exception as e:  # noqa: BLE001 - échec étage 1 : on bascule sur l'OCR
            warnings.append(f"Étage 1 (MarkItDown) en échec pour {filename} : {e}")
            logger.warning("MarkItDown en échec pour %s (%s) — bascule OCR", filename, e)

        ratio = len(markdown) / pages if pages > 0 else 0.0
        if ratio < MIN_CHARS_PER_PAGE:
            warnings.append(
                f"Ratio faible ({ratio:.1f} caractères/page, seuil {MIN_CHARS_PER_PAGE}) — bascule OCR"
            )
            return self._get_ocr().convert(content, filename, pages=pages, warnings=warnings)

        return {
            "markdown": markdown,
            "method": "markitdown",
            "pages_processed": pages,
            "warnings": warnings,
        }
