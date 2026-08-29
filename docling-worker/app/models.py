"""Modèles de données pour le pipeline d'extraction de documents.

Deux niveaux de modèles :
  - **Raw** (RawPageModel, RawBlock, ...) : représentation fidèle de l'extraction PyMuPDF,
    sans aucune décision sémantique. Zone tampon entre le PDF et l'analyseur de structure.
  - **AST** (CanonicalDocument, DocumentElement, ...) : représentation canonique du document,
    issue du StructureAnalyzer. Source de vérité pour le rendu Markdown et le chunking.

La séparation Raw/AST permet de changer les heuristiques sans toucher l'extraction.
"""
from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Raw models — extraction brute, avant analyse
# ---------------------------------------------------------------------------

class RawSpan(BaseModel):
    """Un span de texte dans une ligne PyMuPDF."""
    text: str
    font: str = ""          # ex: "Arial-BoldMT"
    size: float = 0.0       # taille en points
    flags: int = 0          # bitmask PyMuPDF (bit 4=bold, bit 5=italic, ...)
    bbox: list[float] = Field(default_factory=lambda: [0, 0, 0, 0])


class RawLine(BaseModel):
    """Une ligne de texte (contient des spans)."""
    bbox: list[float] = Field(default_factory=lambda: [0, 0, 0, 0])
    spans: list[RawSpan] = Field(default_factory=list)


class RawBlock(BaseModel):
    """Un bloc brut extrait de PyMuPDF (texte ou image)."""
    id: str = ""                    # ex: "b00", "b01"
    block_type: int = 0             # 0=text, 1=image, 2=...
    bbox: list[float] = Field(default_factory=lambda: [0, 0, 0, 0])
    text: str = ""                  # texte concaténé des spans
    lines: list[RawLine] = Field(default_factory=list)
    font_sizes: list[float] = Field(default_factory=list)   # tailles uniques
    fonts: list[str] = Field(default_factory=list)           # polices uniques
    bold_ratio: float = 0.0         # proportion de texte en gras
    span_count: int = 0             # nombre total de spans
    is_noise: bool = False          # True si le bloc est un header/footer ou numéro de page


class RawImage(BaseModel):
    """Image embarquée extraite d'une page."""
    xref: int = 0
    bbox: list[float] = Field(default_factory=lambda: [0, 0, 0, 0])
    width: int = 0
    height: int = 0
    content_type: str = ""
    content: bytes = b""

    model_config = {"arbitrary_types_allowed": True}


class RawDrawing(BaseModel):
    """Un path vectoriel extrait de PyMuPDF (bordure, ligne, rectangle)."""
    type: str = ""                  # "re" (rectangle), "l" (ligne), "c" (courbe)
    bbox: list[float] = Field(default_factory=lambda: [0, 0, 0, 0])
    color: list[float] | None = None
    fill: list[float] | None = None


class RawPageModel(BaseModel):
    """Représentation brute d'une page PDF, extraite sans décision sémantique."""
    page_num: int = 1               # 1-indexed
    width: float = 0.0
    height: float = 0.0
    blocks: list[RawBlock] = Field(default_factory=list)
    images: list[RawImage] = Field(default_factory=list)
    drawings: list[RawDrawing] = Field(default_factory=list)


# ---------------------------------------------------------------------------
# Classification page par page
# ---------------------------------------------------------------------------

class PageType(str, Enum):
    NATIVE = "native"
    HYBRID = "hybrid"
    SCANNED = "scanned"


class PageClassification(BaseModel):
    """Classification d'une page en native/hybrid/scanned."""
    page_num: int = 1
    page_type: PageType = PageType.NATIVE
    text_density: float = 0.0       # chars / page_area
    native_text_ratio: float = 0.0  # chars extraits / expected_chars
    image_ratio: float = 0.0        # aire images / page_area
    block_count: int = 0


# ---------------------------------------------------------------------------
# Canonical AST — représentation canonique du document
# ---------------------------------------------------------------------------

class ElementType(str, Enum):
    HEADING = "heading"
    PARAGRAPH = "paragraph"
    LIST = "list"
    TABLE = "table"
    FIGURE = "figure"
    CAPTION = "caption"
    CODE = "code"
    QUOTE = "quote"


class TableData(BaseModel):
    """Données structurées d'un tableau détecté."""
    headers: list[str] = Field(default_factory=list)
    rows: list[list[str]] = Field(default_factory=list)


class DocumentElement(BaseModel):
    """Un élément structuré du document (heading, paragraph, table, figure, ...)."""
    id: str = ""                     # ex: "p3-e01"
    type: ElementType = ElementType.PARAGRAPH
    level: int | None = None         # 1-6 pour headings, null sinon
    text: str = ""
    bbox: list[float] = Field(default_factory=lambda: [0, 0, 0, 0])
    page: int = 1
    parent_id: str | None = None     # null pour les éléments hors section
    confidence: float = 1.0          # score de confiance de la classification
    table_data: TableData | None = None  # données structurées pour les tableaux
    image_id: str | None = None      # ex: "img_001" — uniquement pour FIGURE


class PageAST(BaseModel):
    """Éléments structurés d'une page."""
    page: int = 1
    elements: list[DocumentElement] = Field(default_factory=list)


class ImageRef(BaseModel):
    """Référence à une image extraite du document."""
    placeholder_id: str = ""         # ex: "img_001"
    content_type: str = ""
    data_base64: str = ""
    caption: str = ""
    bbox: list[float] = Field(default_factory=lambda: [0, 0, 0, 0])
    page: int = 1


class CanonicalDocument(BaseModel):
    """Document canonique — source de vérité pour le rendu et le chunking."""
    pages: list[PageAST] = Field(default_factory=list)
    images: list[ImageRef] = Field(default_factory=list)


# ---------------------------------------------------------------------------
# Réponse du worker
# ---------------------------------------------------------------------------

class ConversionResult(BaseModel):
    """Réponse JSON du endpoint /v1/convert."""
    document: CanonicalDocument | None = None
    markdown: str = ""
    method: str = "unknown"
    pages_processed: int = 0
    images: list[dict] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
