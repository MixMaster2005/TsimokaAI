"""Heuristic classifier for PDF pages: native, hybrid, or scanned.

Uses text density, image ratio, and block count to classify each page.
Thresholds are configurable via environment variables.
"""
from __future__ import annotations

import logging
import os

from app.models import PageClassification, PageType, RawPageModel

logger = logging.getLogger(__name__)

_SCANNED_TEXT_DENSITY = float(os.environ.get("DOCLING_SCANNED_TEXT_DENSITY", 0.01))
_HYBRID_TEXT_DENSITY = float(os.environ.get("DOCLING_HYBRID_TEXT_DENSITY", 0.05))
_SCANNED_IMAGE_RATIO = float(os.environ.get("DOCLING_SCANNED_IMAGE_RATIO", 0.3))


def classify_page(raw_page: RawPageModel) -> PageClassification:
    """Classify a single PDF page as native, hybrid, or scanned.

    Signals:
        - total_text: characters in text blocks (block_type == 0)
        - page_area: width * height in square points
        - text_density: chars / page_area
        - image_area: sum of w * h for embedded images
        - image_ratio: image_area / page_area
        - block_count: number of text blocks
    """
    text_blocks = [b for b in raw_page.blocks if b.block_type == 0]
    total_text = sum(len(b.text) for b in text_blocks)
    block_count = len(text_blocks)

    page_area = raw_page.width * raw_page.height
    if page_area <= 0:
        logger.warning("Page %s has zero/negative area — defaulting to NATIVE", raw_page.page_num)
        return PageClassification(
            page_num=raw_page.page_num,
            page_type=PageType.NATIVE,
            text_density=0.0,
            native_text_ratio=0.0,
            image_ratio=0.0,
            block_count=block_count,
        )

    text_density = total_text / page_area

    image_area = sum(
        min(img.width, raw_page.width) * min(img.height, raw_page.height)
        for img in raw_page.images
    )
    image_ratio = min(image_area / page_area, 1.0)

    logger.debug(
        "Page %s: text_density=%.4f, image_ratio=%.2f, blocks=%d",
        raw_page.page_num,
        text_density,
        image_ratio,
        block_count,
    )

    if block_count >= 3 or text_density > _HYBRID_TEXT_DENSITY:
        page_type = PageType.NATIVE
    elif text_density < _SCANNED_TEXT_DENSITY and image_ratio > _SCANNED_IMAGE_RATIO:
        page_type = PageType.SCANNED
    else:
        page_type = PageType.HYBRID

    return PageClassification(
        page_num=raw_page.page_num,
        page_type=page_type,
        text_density=text_density,
        native_text_ratio=0.0,
        image_ratio=image_ratio,
        block_count=block_count,
    )


def classify_document(raw_pages: list[RawPageModel]) -> list[PageClassification]:
    """Classify all pages in a document. Returns one PageClassification per page."""
    return [classify_page(p) for p in raw_pages]
