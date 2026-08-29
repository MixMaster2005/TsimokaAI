"""Extraction du layout brut d'un PDF via PyMuPDF (fitz).

Convertit un PDF en ``list[RawPageModel]`` en utilisant les blocs de texte,
les images embarquées et les paths vectoriels (dessins).  C'est la première
étape du pipeline : aucune décision sémantique n'est prise ici.
"""
import logging

import fitz

from app.models import (
    RawBlock,
    RawDrawing,
    RawImage,
    RawLine,
    RawPageModel,
    RawSpan,
)

logger = logging.getLogger(__name__)

# Formats d'image supportés (extension -> content_type).
_SUPPORTED_IMAGE_TYPES: dict[str, str] = {
    "png": "image/png",
    "jpeg": "image/jpeg",
    "jpg": "image/jpeg",
    "gif": "image/gif",
    "webp": "image/webp",
    "tiff": "image/tiff",
    "bmp": "image/bmp",
}

# Dimensions minimales en pixels pour qu'une image soit conservée.
_MIN_IMAGE_DIMENSION = 64

# Taille minimale en points pour qu'un dessin soit conservé.
_MIN_DRAWING_SIZE = 5.0


# ---------------------------------------------------------------------------
# Point d'entrée principal
# ---------------------------------------------------------------------------

def extract_raw_document(content: bytes) -> list[RawPageModel]:
    """Ouvre un PDF et retourne une ``RawPageModel`` par page."""
    doc = fitz.open(stream=content, filetype="pdf")
    try:
        pages: list[RawPageModel] = []
        for page_index, page in enumerate(doc):
            pages.append(_extract_page(page, doc, page_index))
        return pages
    finally:
        doc.close()


# ---------------------------------------------------------------------------
# Extraction par page
# ---------------------------------------------------------------------------

def _extract_page(page, doc, page_index: int) -> RawPageModel:
    """Extrait le contenu brut d'une seule page PDF."""
    width = float(page.rect.width)
    height = float(page.rect.height)

    blocks: list[RawBlock] = []
    images: list[RawImage] = []
    drawings: list[RawDrawing] = []

    # --- Blocs de texte ---
    try:
        page_dict = page.get_text("dict", flags=fitz.TEXT_PRESERVE_WHITESPACE)  # type: ignore[union-attr]
        for block_index, block_dict in enumerate(page_dict.get("blocks", [])):
            try:
                raw_block = _parse_text_block(block_dict, block_index)
                if raw_block is not None:
                    blocks.append(raw_block)
            except Exception:
                logger.debug(
                    "Bloc %d (page %d) ignoré — erreur d'interprétation",
                    block_index, page_index + 1,
                )
    except Exception:
        logger.warning("Échec get_text('dict') page %d", page_index + 1)

    # --- Images ---
    try:
        images = _extract_page_images(page, doc)
    except Exception:
        logger.warning("Échec extraction images page %d", page_index + 1)

    # --- Dessins vectoriels ---
    try:
        drawings = _extract_drawings(page)
    except Exception:
        logger.warning("Échec extraction dessins page %d", page_index + 1)

    return RawPageModel(
        page_num=page_index + 1,
        width=width,
        height=height,
        blocks=blocks,
        images=images,
        drawings=drawings,
    )


# ---------------------------------------------------------------------------
# Blocs de texte
# ---------------------------------------------------------------------------

def _parse_text_block(block_dict: dict, block_index: int) -> RawBlock | None:
    """Convertit un bloc PyMuPDF en ``RawBlock``."""
    block_type = block_dict.get("type", 0)

    # On ne traite que les blocs texte (type 0). Les blocs image (type 1)
    # sont gérés séparément par _extract_page_images.
    if block_type != 0:
        return None

    bbox = block_dict.get("bbox", [0, 0, 0, 0])
    lines_data: list[RawLine] = []
    all_font_sizes: list[float] = []
    all_fonts: list[str] = []
    total_bold_chars = 0
    total_chars = 0
    total_spans = 0

    for line_dict in block_dict.get("lines", []):
        line_bbox = line_dict.get("bbox", [0, 0, 0, 0])
        raw_spans: list[RawSpan] = []

        for span_dict in line_dict.get("spans", []):
            text = span_dict.get("text", "")
            font = span_dict.get("font", "")
            size = span_dict.get("size", 0.0)
            flags = span_dict.get("flags", 0)
            span_bbox = span_dict.get("bbox", [0, 0, 0, 0])

            raw_spans.append(RawSpan(
                text=text,
                font=font,
                size=size,
                flags=flags,
                bbox=span_bbox,
            ))

            total_spans += 1
            char_count = len(text)
            total_chars += char_count

            if size > 0:
                all_font_sizes.append(size)
            if font:
                all_fonts.append(font)

            # Bit 4 (valeur 16) = bold dans la convention PyMuPDF.
            is_bold = (flags & 16) != 0 or "Bold" in font
            if is_bold:
                total_bold_chars += char_count

        lines_data.append(RawLine(bbox=line_bbox, spans=raw_spans))

    # Texte complet : chaque span sur la même ligne est collé, chaque ligne
    # est séparée par un saut de ligne.
    full_text = "\n".join(
        "".join(span.text for span in line.spans)
        for line in lines_data
    )

    unique_sizes = sorted(set(all_font_sizes))
    unique_fonts = list(dict.fromkeys(all_fonts))  # preserve order, dedup
    bold_ratio = (total_bold_chars / total_chars) if total_chars > 0 else 0.0

    return RawBlock(
        id=f"b{block_index:02d}",
        block_type=block_type,
        bbox=bbox,
        text=full_text,
        lines=lines_data,
        font_sizes=unique_sizes,
        fonts=unique_fonts,
        bold_ratio=bold_ratio,
        span_count=total_spans,
    )


# ---------------------------------------------------------------------------
# Images embarquées
# ---------------------------------------------------------------------------

def _extract_page_images(page, doc) -> list[RawImage]:
    """Extrait les images embarquées d'une page."""
    images: list[RawImage] = []

    for image_info in page.get_images(full=True):
        xref = image_info[0]
        try:
            extracted = doc.extract_image(xref)
            if not extracted:
                continue

            width = image_info[2] or 0
            height = image_info[3] or 0

            if width < _MIN_IMAGE_DIMENSION or height < _MIN_IMAGE_DIMENSION:
                logger.debug(
                    "Image xref=%s ignorée : trop petite (%dx%d)",
                    xref, width, height,
                )
                continue

            ext = extracted.get("ext", "")
            content_type = _SUPPORTED_IMAGE_TYPES.get(ext)
            if content_type is None:
                logger.debug(
                    "Image xref=%s ignorée : format '%s' non supporté",
                    xref, ext,
                )
                continue

            # Bounding box sur la page (fallback sur les rects connus).
            try:
                img_rects = page.get_image_rects(xref)
                if img_rects:
                    r = img_rects[0]
                    img_bbox = [r.x0, r.y0, r.x1, r.y1]
                else:
                    img_bbox = [0, 0, width, height]
            except Exception:
                img_bbox = [0, 0, width, height]

            images.append(RawImage(
                xref=xref,
                bbox=img_bbox,
                width=width,
                height=height,
                content_type=content_type,
                content=extracted["image"],
            ))
        except Exception:
            logger.debug("Image xref=%s non extraite — ignorée", xref)

    return images


# ---------------------------------------------------------------------------
# Dessins vectoriels (paths)
# ---------------------------------------------------------------------------

def _extract_drawings(page) -> list[RawDrawing]:
    """Extrait les paths vectoriels (rectangles, lignes) d'une page."""
    drawings: list[RawDrawing] = []

    for item in page.get_drawings():
        kind = item.get("type", "")
        rect = item.get("rect")
        if rect is None:
            continue

        # rect est un fitz.Rect → convertir en liste.
        try:
            bbox = [rect.x0, rect.y0, rect.x1, rect.y1]
        except Exception:
            continue

        w = bbox[2] - bbox[0]
        h = bbox[3] - bbox[1]

        # Ignorer les éléments trop petits.
        if w < _MIN_DRAWING_SIZE or h < _MIN_DRAWING_SIZE:
            continue

        # On ne garde que les rectangles ("re") et les lignes ("l").
        if kind not in ("re", "l"):
            continue

        color = item.get("color")
        fill = item.get("fill")

        # Normaliser les couleurs : fitz peut retourner un float simple ou un tuple.
        if isinstance(color, (int, float)):
            color = [color]
        if isinstance(fill, (int, float)):
            fill = [fill]

        drawings.append(RawDrawing(
            type=kind,
            bbox=bbox,
            color=list(color) if color is not None else None,
            fill=list(fill) if fill is not None else None,
        ))

    return drawings
