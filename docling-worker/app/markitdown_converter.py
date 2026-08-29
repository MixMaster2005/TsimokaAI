"""Étage 1 — conversion document + enrichissement vision (Gemini).

Architecture v3 — routage PDF / non-PDF :

  - **PDF** : pipeline PyMuPDF natif
    1. Extraction brute via ``layout_extractor`` (PyMuPDF blocks/images/drawings).
    2. Classification page par page (``page_classifier`` : native/hybrid/scanned).
    3. Analyse structurelle → AST canonique (``structure_analyzer``).
    4. Extraction images embarquées + légende Gemini (``image_extractor`` + ``vision_captioner``).
    5. Rendu Markdown à partir de l'AST (``markdown_renderer``).

  - **Non-PDF** (DOCX/PPTX/XLSX/HTML/CSV/…) : MarkItDown classique
    1. Conversion MarkItDown → Markdown structuré, CPU-only.
    2. Extraction des images embarquées + légende Gemini.

L'échec de Gemini n'est jamais bloquant : captions/transcriptions dégradées + warning.
"""
import base64
import io
import logging
import os
import re

from markitdown import MarkItDown
from pypdf import PdfReader

from app import image_extractor
from app.image_extractor import ExtractedImage
from app.models import CanonicalDocument
from app.vision_captioner import VisionCaptioner

logger = logging.getLogger(__name__)

MIN_CHARS_PER_PAGE = int(os.environ.get("DOCLING_MIN_CHARS_PER_PAGE", "40"))
PAGE_SEPARATOR = "\n\n---\n\n"

# Marqueurs d'image insérés par MarkItDown (DOCX -> data-URI base64, PPTX -> chemin).
IMAGE_MARKER_RE = re.compile(r"!\[[^\]]*\]\([^)]*\)")


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


class DocumentConverter:
    """Convertisseur de documents : routage PDF (pipeline PyMuPDF) / non-PDF (MarkItDown)."""

    def __init__(self) -> None:
        self._md = MarkItDown()
        self._vision = VisionCaptioner()
        # Pipeline PDF
        from app.structure_analyzer import StructureAnalyzer
        self._analyzer = StructureAnalyzer()

    def convert(self, content: bytes, filename: str) -> dict:
        warnings: list = []
        extension = os.path.splitext(filename)[1].lower()

        if extension == ".pdf":
            return self._convert_pdf(content, filename, warnings)
        else:
            return self._convert_with_markitdown(content, filename, warnings)

    # ------------------------------------------------------------------
    # PDF pipeline (PyMuPDF → AST → Markdown)
    # ------------------------------------------------------------------

    def _convert_pdf(self, content: bytes, filename: str, warnings: list) -> dict:
        from app import layout_extractor
        from app import markdown_renderer
        from app import page_classifier

        # 1. Extraction brute via PyMuPDF
        raw_pages = layout_extractor.extract_raw_document(content)

        # 2. Classification page par page
        classifications = page_classifier.classify_document(raw_pages)

        # 3. Analyse structurelle → AST
        document = self._analyzer.analyze(raw_pages, classifications)

        # 4. Extraction images embarquées + légende Gemini
        images = self._extract_and_caption_images(document, content, filename, warnings)

        # 5. Rendu Markdown à partir de l'AST
        markdown = markdown_renderer.render(document)

        return {
            "document": document.model_dump(),
            "markdown": markdown,
            "method": "pymupdf_layout",
            "pages_processed": len(raw_pages),
            "images": images,
            "warnings": warnings,
        }

    def _extract_and_caption_images(
        self,
        document: CanonicalDocument,
        content: bytes,
        filename: str,
        warnings: list,
    ) -> list[dict]:
        """Extraction images via image_extractor + légende Gemini.

        Utilise les placeholder_id pré-assignés par le StructureAnalyzer
        (document.images) comme source de vérité pour les IDs. Les images
        extraites par image_extractor sont appariées par position à celles
        de l'AST.
        """
        extracted = image_extractor.extract_images(content, filename)
        ast_refs = document.images  # pré-assignés par le StructureAnalyzer
        images = []

        for index, image in enumerate(extracted):
            if index >= image_extractor.MAX_EXTRACTED_IMAGES:
                warnings.append(
                    f"Plafond atteint : seules les {image_extractor.MAX_EXTRACTED_IMAGES} "
                    "premières images sont légendées"
                )
                break
            # Utiliser l'placeholder_id de l'AST si disponible
            if index < len(ast_refs):
                placeholder_id = ast_refs[index].placeholder_id
            else:
                placeholder_id = f"img_{index + 1:03d}"
            caption = ""
            try:
                caption = self._vision.caption_figure(image.content, image.content_type)
            except Exception as e:  # noqa: BLE001 - Gemini indisponible : légende vide + warning
                warnings.append(f"Légende Gemini impossible pour {image.location} : {e}")
            images.append({
                "placeholder_id": placeholder_id,
                "content_type": image.content_type,
                "data_base64": base64.b64encode(image.content).decode("ascii"),
                "caption": caption,
            })
        return images

    # ------------------------------------------------------------------
    # Non-PDF pipeline (MarkItDown)
    # ------------------------------------------------------------------

    def _convert_with_markitdown(self, content: bytes, filename: str, warnings: list) -> dict:
        pages = count_pages(content, filename)
        markdown = self._convert_markitdown(content, filename, warnings)
        return self._caption_figures(markdown, content, filename, pages, warnings)

    def _convert_markitdown(self, content: bytes, filename: str, warnings: list) -> str:
        markdown = ""
        try:
            extension = os.path.splitext(filename)[1]
            markdown = self._md.convert_stream(
                io.BytesIO(content), file_extension=extension
            ).text_content or ""
        except Exception as e:  # noqa: BLE001 - échec étage 1 : signalé en warning
            warnings.append(f"Étage 1 (MarkItDown) en échec pour {filename} : {e}")
            logger.warning("MarkItDown en échec pour %s (%s)", filename, e)
        return markdown

    def _is_scanned(self, markdown: str, pages: int) -> bool:
        ratio = len(markdown) / pages if pages > 0 else 0.0
        return ratio < MIN_CHARS_PER_PAGE

    def _transcribe_pages(self, content: bytes, filename: str, pages: int, warnings: list) -> dict:
        """Document scanné (ratio trop faible) : rendu des pages + transcription Gemini."""
        if os.path.splitext(filename)[1].lower() != ".pdf":
            warnings.append(
                "Ratio caractères/page faible mais format non PDF — transcription par page impossible"
            )
            return {
                "markdown": "",
                "method": "markitdown_with_page_transcription",
                "pages_processed": pages,
                "images": [],
                "warnings": warnings,
            }

        try:
            page_images = image_extractor.render_pdf_pages(content)
        except Exception as e:  # noqa: BLE001 - PDF illisible en rendu
            warnings.append(f"Rendu des pages impossible : {e}")
            return {
                "markdown": "",
                "method": "markitdown_with_page_transcription",
                "pages_processed": pages,
                "images": [],
                "warnings": warnings,
            }

        transcribed = []
        for index, page_image in enumerate(page_images, start=1):
            try:
                text = self._vision.transcribe_full_page(page_image)
                if text:
                    transcribed.append(text)
                logger.info("Page %d transcrite par Gemini (%d caractères)", index, len(text))
            except Exception as e:  # noqa: BLE001 - Gemini indisponible : warning non bloquant
                warnings.append(f"Transcription Gemini de la page {index} en échec : {e}")

        return {
            "markdown": PAGE_SEPARATOR.join(transcribed),
            "method": "markitdown_with_page_transcription",
            "pages_processed": pages,
            "images": [],
            "warnings": warnings,
        }

    def _caption_figures(self, markdown: str, content: bytes, filename: str,
                         pages: int, warnings: list) -> dict:
        """Document textuel : extraction des images embarquées + légende Gemini."""
        extracted: list[ExtractedImage] = image_extractor.extract_images(content, filename)
        placeholders: list[tuple[str, str]] = []
        images: list[dict] = []

        for index, image in enumerate(extracted, start=1):
            if index > image_extractor.MAX_EXTRACTED_IMAGES:
                warnings.append(
                    f"Plafond atteint : seules les {image_extractor.MAX_EXTRACTED_IMAGES} premières "
                    "images sont légendées, les suivantes sont ignorées"
                )
                break
            placeholder_id = f"img_{index:03d}"
            caption = ""
            try:
                caption = self._vision.caption_figure(image.content, image.content_type)
            except Exception as e:  # noqa: BLE001 - Gemini indisponible : légende vide + warning
                warnings.append(f"Légende Gemini impossible pour {image.location} : {e}")
                logger.warning("Légende Gemini impossible pour %s (%s)", image.location, e)

            images.append({
                "placeholder_id": placeholder_id,
                "content_type": image.content_type,
                "data_base64": base64.b64encode(image.content).decode("ascii"),
                "caption": caption,
            })
            placeholders.append((placeholder_id, "{{IMAGE:%s}}" % placeholder_id))

        markdown = _inject_placeholders(markdown, placeholders)
        return {
            "markdown": markdown,
            "method": "markitdown",
            "pages_processed": pages,
            "images": images,
            "warnings": warnings,
        }


# ------------------------------------------------------------------
# Helpers
# ------------------------------------------------------------------


def _inject_placeholders(markdown: str, placeholders: list[tuple[str, str]]) -> str:
    """Remplace les marqueurs ``![](...)`` de MarkItDown par les placeholders, dans l'ordre ;
    les placeholders sans marqueur (cas PDF) sont collés à la fin du document."""
    if not placeholders:
        return markdown

    markers = list(IMAGE_MARKER_RE.finditer(markdown))
    if not markers:
        suffix = "\n\n" + "\n\n".join(text for _, text in placeholders)
        return markdown.rstrip() + suffix

    parts = []
    last = 0
    for marker, (_, text) in zip(markers, placeholders):
        parts.append(markdown[last:marker.start()])
        parts.append(text)
        last = marker.end()
    parts.append(markdown[last:])

    leftover = placeholders[len(markers):]
    result = "".join(parts)
    if leftover:
        result = result.rstrip() + "\n\n" + "\n\n".join(text for _, text in leftover)
    return result


# Rétrocompatibilité : les anciens importaient MarkItDownConverter
MarkItDownConverter = DocumentConverter
