"""Rendu Markdown à partir du CanonicalDocument AST.

Fonction pure : même AST → même Markdown à chaque fois.
"""
from __future__ import annotations

import logging
import re

from app.models import (
    CanonicalDocument,
    DocumentElement,
    ElementType,
)

logger = logging.getLogger(__name__)


def render(document: CanonicalDocument) -> str:
    parts = []
    for page in document.pages:
        for element in page.elements:
            rendered = _render_element(element)
            if rendered:
                parts.append(rendered)
    result = "".join(parts)
    result = re.sub(r"\n{3,}", "\n\n", result)
    return result.strip() + "\n"


def _render_element(element: DocumentElement) -> str:
    renderers = {
        ElementType.HEADING: _render_heading,
        ElementType.PARAGRAPH: _render_paragraph,
        ElementType.LIST: _render_list,
        ElementType.TABLE: _render_table,
        ElementType.FIGURE: _render_figure,
        ElementType.CAPTION: _render_caption,
        ElementType.CODE: _render_code,
        ElementType.QUOTE: _render_quote,
    }
    renderer = renderers.get(element.type)
    if renderer:
        return renderer(element)
    return f"{element.text}\n\n"


def _render_heading(element: DocumentElement) -> str:
    if not element.text:
        return ""
    level = element.level or 1
    prefix = "#" * level
    return f"{prefix} {element.text}\n\n"


def _render_paragraph(element: DocumentElement) -> str:
    if not element.text:
        return ""
    return f"{element.text}\n\n"


def _render_list(element: DocumentElement) -> str:
    if not element.text:
        return ""
    lines = element.text.strip().split("\n")
    items = []
    for line in lines:
        line = line.strip()
        if line:
            if re.match(r"^[-•*]\s|^\d+[.)]\s", line):
                items.append(line)
            else:
                items.append(f"- {line}")
    return "\n".join(items) + "\n\n"


def _render_table(element: DocumentElement) -> str:
    if not element.table_data:
        return f"{element.text}\n\n" if element.text else ""

    headers = element.table_data.headers
    rows = element.table_data.rows

    if not headers:
        return f"{element.text}\n\n" if element.text else ""

    def _escape_cell(text: str) -> str:
        """Échapper les pipes et nettoyer le contenu d'une cellule."""
        t = text.replace("|", "\\|").replace("\n", " ").strip()
        return re.sub(r"\s+", " ", t)

    # Normaliser le nombre de colonnes
    n_cols = len(headers)
    clean_headers = [_escape_cell(h) for h in headers]
    data_lines = []
    for row in rows:
        padded = row + [""] * (n_cols - len(row))
        clean_row = [_escape_cell(c) for c in padded[:n_cols]]
        data_lines.append("| " + " | ".join(clean_row) + " |")

    header_line = "| " + " | ".join(clean_headers) + " |"
    separator = "| " + " | ".join(["---"] * n_cols) + " |"
    return "\n".join([header_line, separator] + data_lines) + "\n\n"


def _render_figure(element: DocumentElement) -> str:
    caption = element.text if element.text else "Figure extraite du document"
    if not element.image_id:
        raise ValueError(f"FIGURE element {element.id} sans image_id")
    return f"![{caption}]({{IMAGE:{element.image_id}}})\n\n"


def _render_caption(element: DocumentElement) -> str:
    if not element.text:
        return ""
    return f"*{element.text}*\n\n"


def _render_code(element: DocumentElement) -> str:
    if not element.text:
        return ""
    return f"```\n{element.text}\n```\n\n"


def _render_quote(element: DocumentElement) -> str:
    if not element.text:
        return ""
    lines = element.text.strip().split("\n")
    quoted = "\n".join(f"> {line}" for line in lines)
    return quoted + "\n\n"
