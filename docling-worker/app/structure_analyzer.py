"""Analyse structurelle des pages PDF pour produire un CanonicalDocument.

Transforme ``list[RawPageModel]`` + ``list[PageClassification]`` en un
``CanonicalDocument`` AST contenant headings, tableaux, paragraphes, etc.
"""
from __future__ import annotations

import logging
import re
from collections import Counter

from app.models import (
    CanonicalDocument,
    DocumentElement,
    ElementType,
    ImageRef,
    PageAST,
    PageClassification,
    PageType,
    RawBlock,
    RawDrawing,
    RawImage,
    RawPageModel,
    TableData,
)

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

HEADING_THRESHOLD = 3.5

_TABLE_WEIGHTS = {
    "alignment": 0.35,
    "repetition": 0.30,
    "borders": 0.15,
    "density": 0.20,
}

_LIST_BULLET_RE = re.compile(
    r"^[\u2022\u2023\u25E6\u2043\u2219\u203F\u2040\u25AA\u25AB\u25FB\u25FC\u25FD\u25FE]"
    r"|^\-\s|^\*\s|^\+\s"
)
_NUMBERED_LIST_RE = re.compile(r"^\d+[\.\)]\s")
_MULTILINE_CHARS = re.compile(r"[\n\u2028\u2029]")
_MONOSPACE_FONTS = frozenset({
    "courier", "consolas", "monaco", "lucidaconsole", "dejavusansmono",
    "liberationmono", "nimbusmono", "droid Sans Mono", "sourcecodepro",
    "firamono", "inconsolata", "ubuntu mono",
})

# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------


class StructureAnalyzer:
    """Pipeline principal : RawPages + Classifications → CanonicalDocument."""

    def __init__(self) -> None:
        self._heading_detector = HeadingDetector()
        self._table_detector = TableDetector()

    def analyze(
        self,
        raw_pages: list[RawPageModel],
        classifications: list[PageClassification],
    ) -> CanonicalDocument:
        # Phase 0: collect every font size across the full document
        all_sizes = _collect_all_sizes(raw_pages)

        pages_ast: list[PageAST] = []
        all_image_refs: list[ImageRef] = []
        global_img_idx = 0

        for page, classification in zip(raw_pages, classifications):
            if classification.page_type == PageType.SCANNED:
                pages_ast.append(_scanned_page_ast(page.page_num))
                continue

            text_blocks = [b for b in page.blocks if b.block_type == 0 and not b.is_noise]

            # Detect headings
            heading_elements, heading_block_ids = self._heading_detector.detect(
                text_blocks, page.page_num, all_sizes, page.height,
            )

            # Detect tables (exclude blocks already consumed by heading detector)
            remaining_after_headings = [b for b in text_blocks if b.id not in heading_block_ids]
            table_elements, table_block_ids = self._table_detector.detect(
                remaining_after_headings, page.drawings, page.page_num, page.width, page.height,
            )

            # Classify the rest
            remaining = _classify_remaining_blocks(
                text_blocks, heading_block_ids, table_block_ids, page.page_num,
            )

            # Create FIGURE elements for embedded images
            figure_elements, img_refs, global_img_idx = _create_figure_elements(
                page, page.page_num, global_img_idx,
            )
            all_image_refs.extend(img_refs)

            # Merge all elements, sorted by position then type priority
            all_elements = heading_elements + table_elements + remaining + figure_elements
            all_elements.sort(key=lambda e: (e.page, e.bbox[1], e.bbox[0]))

            # Build hierarchy
            all_elements = _build_parent_ids(all_elements)

            pages_ast.append(PageAST(page=page.page_num, elements=all_elements))

        return CanonicalDocument(pages=pages_ast, images=all_image_refs)


# ---------------------------------------------------------------------------
# HeadingDetector
# ---------------------------------------------------------------------------


class HeadingDetector:
    """Détection de titres par scoring multi-signal."""

    def detect(
        self,
        blocks: list[RawBlock],
        page_num: int,
        all_document_sizes: list[float],
        page_height: float,
    ) -> tuple[list[DocumentElement], set[str]]:
        if not blocks:
            return [], set()

        sorted_sizes = _unique_sorted_desc(all_document_sizes)
        prev_block: RawBlock | None = None

        # Phase A: score each text block
        candidates: list[tuple[RawBlock, float]] = []
        for block in blocks:
            score = (
                _font_size_score(block, sorted_sizes)
                + _bold_score(block)
                + _whitespace_score(block, prev_block, page_height)
                + _length_score(block)
                + _numbering_score(block)
                + _position_score(block, page_height)
            )
            if score >= HEADING_THRESHOLD:
                text = block.text.strip()
                # Nettoyer les sauts de ligne dans les headings
                text = _MULTILINE_CHARS.sub(" ", text)
                text = re.sub(r"\s+", " ", text).strip()
                word_count = len(text.split())
                if word_count <= 1 and _numbering_score(block) == 0:
                    prev_block = block
                    continue
                if re.match(r'^\d{1,3}$', text) and block.bbox[1] > page_height * 0.8:
                    prev_block = block
                    continue
                if len(text) < 5 and block.bold_ratio < 0.5 and _numbering_score(block) == 0:
                    prev_block = block
                    continue
                # Filtre : items de liste numérotés non gras (ex: "1. Remplir le formulaire...")
                if _numbering_score(block) > 0 and block.bold_ratio < 0.5 and word_count > 5:
                    prev_block = block
                    continue
                candidates.append((block, score))
            prev_block = block

        if not candidates:
            return [], set()

        # Phase B: exclude table column headers (short blocks in aligned groups)
        candidates = _filter_table_headers(candidates, blocks)
        if not candidates:
            return [], set()

        # Phase C: deduplicate — keep best-scoring block per normalized text
        candidates = _deduplicate_heading_candidates(candidates)

        # Phase D: assign heading levels by font size ranking
        candidate_sizes = sorted(
            {_primary_font_size(b) for b, _ in candidates},
            reverse=True,
        )
        size_to_level: dict[float, int] = {}
        for idx, sz in enumerate(candidate_sizes):
            size_to_level[sz] = min(idx + 1, 6)

        elements: list[DocumentElement] = []
        consumed_block_ids: set[str] = set()
        for idx, (block, score) in enumerate(candidates):
            level = size_to_level[_primary_font_size(block)]
            clean_text = _MULTILINE_CHARS.sub(" ", block.text.strip())
            clean_text = re.sub(r"\s+", " ", clean_text).strip()
            elements.append(DocumentElement(
                id=f"p{page_num}-h{idx:03d}",
                type=ElementType.HEADING,
                level=level,
                text=clean_text,
                bbox=list(block.bbox),
                page=page_num,
                parent_id=None,
                confidence=min(score / (HEADING_THRESHOLD * 2), 1.0),
            ))
            consumed_block_ids.add(block.id)

        # Phase E: set parent_id (last heading with strictly lower level)
        _assign_heading_parents(elements)

        return elements, consumed_block_ids


def _filter_table_headers(
    candidates: list[tuple[RawBlock, float]],
    all_blocks: list[RawBlock],
) -> list[tuple[RawBlock, float]]:
    """Exclude heading candidates that are likely table column headers.

    Heuristic: if a candidate is short (< 40 chars), non-numbered, and has
    3+ sibling blocks at similar y-coordinate and x0 alignment, it's
    probably a table header row, not a document heading.
    """
    if len(all_blocks) < 4:
        return candidates

    # Build spatial index: group blocks by y-proximity
    sorted_blocks = sorted(all_blocks, key=lambda b: (b.bbox[1], b.bbox[0]))
    y_groups: list[list[RawBlock]] = []
    current_group = [sorted_blocks[0]]
    for block in sorted_blocks[1:]:
        prev = current_group[-1]
        y_gap = abs(block.bbox[1] - prev.bbox[1])
        if y_gap <= 8.0:
            current_group.append(block)
        else:
            if len(current_group) >= 3:
                y_groups.append(current_group)
            current_group = [block]
    if len(current_group) >= 3:
        y_groups.append(current_group)

    # For each y-group, compute x0 clusters
    table_block_ids: set[str] = set()
    for group in y_groups:
        if len(group) < 3:
            continue
        x0s = sorted(b.bbox[0] for b in group)
        clusters = _cluster_values(x0s, tolerance=10.0)
        if len(clusters) >= 2:
            # Multiple columns → likely a table row
            for block in group:
                table_block_ids.add(block.id)

    # Filter: remove candidates whose block is in a table region
    filtered = []
    for block, score in candidates:
        if block.id in table_block_ids:
            text = block.text.strip()
            text = _MULTILINE_CHARS.sub(" ", text)
            # Keep only if it's numbered (e.g. "1. Introduction") or long
            if _numbering_score(block) > 0 or len(text) > 40:
                filtered.append((block, score))
        else:
            filtered.append((block, score))

    return filtered


def _deduplicate_heading_candidates(
    candidates: list[tuple[RawBlock, float]],
) -> list[tuple[RawBlock, float]]:
    """Remove duplicate heading candidates: same normalized text on same page.

    Keeps the block with the highest score.
    """
    if not candidates:
        return []

    # Group by normalized text
    by_text: dict[str, list[tuple[RawBlock, float]]] = {}
    for block, score in candidates:
        text = block.text.strip()
        text = _MULTILINE_CHARS.sub(" ", text)
        text = re.sub(r"\s+", " ", text).strip()
        by_text.setdefault(text, []).append((block, score))

    deduped: list[tuple[RawBlock, float]] = []
    for text, group in by_text.items():
        # Keep the highest-scoring block
        best = max(group, key=lambda x: x[1])
        deduped.append(best)

    return deduped


# ---------------------------------------------------------------------------
# TableDetector
# ---------------------------------------------------------------------------


class TableDetector:
    """Détection de tableaux par regroupement spatial et scoring."""

    def detect(
        self,
        blocks: list[RawBlock],
        drawings: list[RawDrawing],
        page_num: int,
        page_width: float,
        page_height: float,
    ) -> tuple[list[DocumentElement], set[str]]:
        text_blocks = [b for b in blocks if b.block_type == 0]
        if not text_blocks:
            return [], set()

        groups = _group_candidate_blocks(text_blocks, page_height=page_height, y_gap_threshold=25.0)

        elements: list[DocumentElement] = []
        seen_ids: set[str] = set()
        consumed_block_ids: set[str] = set()

        for group in groups:
            if len(group) < 3:
                continue
            # Require at least 2 distinct column positions (x0 clusters)
            x0s = sorted(b.bbox[0] for b in group)
            n_cols = len(_cluster_values(x0s, tolerance=15.0))
            if n_cols < 2:
                continue
            score = _table_score(group, drawings)
            if score > 0.55:
                elem = _build_table_element(group, score, page_num, len(elements))
                if elem.id not in seen_ids:
                    elements.append(elem)
                    seen_ids.add(elem.id)
                    for block in group:
                        consumed_block_ids.add(block.id)
            elif score > 0.25:
                # Ambiguous — treat as paragraph for now with low confidence
                for block in group:
                    if block.id not in consumed_block_ids:
                        elements.append(DocumentElement(
                            id=f"p{page_num}-tbl{len(elements):03d}",
                            type=ElementType.PARAGRAPH,
                            text=block.text.strip(),
                            bbox=list(block.bbox),
                            page=page_num,
                            confidence=score,
                        ))
                        consumed_block_ids.add(block.id)

        return elements, consumed_block_ids


# ---------------------------------------------------------------------------
# Helper functions
# ---------------------------------------------------------------------------


def _scanned_page_ast(page_num: int) -> PageAST:
    """Create a placeholder element for scanned pages."""
    return PageAST(
        page=page_num,
        elements=[DocumentElement(
            id=f"p{page_num}-scan",
            type=ElementType.PARAGRAPH,
            text=f"[Page {page_num} — scanned, requires VLM processing]",
            bbox=[0, 0, 0, 0],
            page=page_num,
            confidence=0.0,
        )],
    )


def _collect_all_sizes(pages: list[RawPageModel]) -> list[float]:
    """Gather every font size from every text block across all pages."""
    sizes: list[float] = []
    for page in pages:
        for block in page.blocks:
            if block.block_type == 0:
                sizes.extend(block.font_sizes)
    return sizes


def _unique_sorted_desc(sizes: list[float]) -> list[float]:
    """Return unique font sizes sorted descending (largest first)."""
    return sorted(set(s for s in sizes if s > 0), reverse=True)


def _primary_font_size(block: RawBlock) -> float:
    """Return the most common font size in the block, or 0."""
    if not block.font_sizes:
        return 0.0
    # font_sizes is already unique, but we want the most common
    # from the lines/spans. Fallback: use the first one.
    size_counts: Counter[float] = Counter()
    for line in block.lines:
        for span in line.spans:
            if span.size > 0:
                size_counts[span.size] += 1
    if size_counts:
        return size_counts.most_common(1)[0][0]
    return block.font_sizes[0] if block.font_sizes else 0.0


# --- Score components (each 0–1) ---


def _font_size_score(block: RawBlock, sorted_doc_sizes: list[float]) -> float:
    """Score based on font size relative to all document sizes."""
    size = _primary_font_size(block)
    if not sorted_doc_sizes or size <= 0:
        return 0.0
    # Percentile ranking: position in descending list
    for idx, doc_size in enumerate(sorted_doc_sizes):
        if size >= doc_size:
            rank = idx / len(sorted_doc_sizes)
            # Top 20% → 0.8+, bottom 50% → <0.2
            return 1.0 - rank
    return 0.0


def _bold_score(block: RawBlock) -> float:
    """Score based on bold ratio (0 = no bold, 1 = all bold)."""
    return min(block.bold_ratio, 1.0)


def _whitespace_score(block: RawBlock, prev_block: RawBlock | None, page_height: float) -> float:
    """Score based on vertical gap above the block."""
    if prev_block is None:
        return 0.5  # first block on page — moderate signal
    avg_line_height = max(
        (line.bbox[3] - line.bbox[1] for line in block.lines if line.bbox[3] > line.bbox[1]),
        default=12.0,
    )
    gap = block.bbox[1] - prev_block.bbox[3]
    if avg_line_height <= 0:
        return 0.0
    ratio = gap / avg_line_height
    if ratio >= 2.0:
        return 1.0
    elif ratio >= 1.0:
        return 0.6
    elif ratio >= 0.5:
        return 0.3
    return 0.0


def _length_score(block: RawBlock) -> float:
    """Shorter text is more likely a heading."""
    n = len(block.text.strip())
    if n <= 0:
        return 0.0
    if n < 50:
        return 1.0
    if n > 200:
        return 0.0
    # Sigmoid-like transition between 50 and 200
    t = (n - 50) / 150.0
    return 1.0 - t


def _numbering_score(block: RawBlock) -> float:
    """Detect heading numbering patterns."""
    text = block.text.strip()
    patterns = [
        r"^\d+(\.\d+)*\s",
        r"^[A-Z]\.\s",
        r"^[IVXLC]+\.\s",
        r"^Chapter\s+\d+",
        r"^Section\s+\d+",
        r"^Article\s+\d+",
        r"^Partie?\s+\d+",
    ]
    for pat in patterns:
        if re.match(pat, text, re.IGNORECASE):
            return 1.0
    return 0.0


def _position_score(block: RawBlock, page_height: float) -> float:
    """Blocks in the top 20% of page score higher."""
    if page_height <= 0:
        return 0.0
    y_ratio = block.bbox[1] / page_height
    if y_ratio <= 0.2:
        return 1.0
    elif y_ratio <= 0.4:
        return 0.5
    elif y_ratio >= 0.85:
        return 0.0
    return 0.2


# --- Parent-id assignment ---


def _build_parent_ids(elements: list[DocumentElement]) -> list[DocumentElement]:
    """Build parent-child hierarchy for all elements.

    Headings are nested by level via ``_assign_heading_parents``.
    Non-heading elements (paragraphs, tables, lists, …) are attached as
    children of the most recent heading that precedes them vertically.
    """
    # 1. Heading hierarchy
    _assign_heading_parents(elements)

    # 2. Attach non-heading elements to nearest preceding heading
    last_heading: DocumentElement | None = None
    for elem in elements:
        if elem.type == ElementType.HEADING:
            last_heading = elem
        elif last_heading is not None and elem.parent_id is None:
            elem.parent_id = last_heading.id

    return elements


# --- Heading level assignment ---


def _assign_heading_parents(elements: list[DocumentElement]) -> None:
    """Set parent_id for each heading: last heading with strictly lower level."""
    stack: list[DocumentElement] = []  # decreasing level stack
    for elem in elements:
        if elem.type != ElementType.HEADING:
            continue
        # Pop stack until we find a parent with strictly lower level
        while stack and stack[-1].level is not None and elem.level is not None:
            if stack[-1].level < elem.level:
                break
            stack.pop()
        if stack:
            elem.parent_id = stack[-1].id
        else:
            elem.parent_id = None
        stack.append(elem)


# --- Block grouping for table detection ---


def _group_candidate_blocks(
    blocks: list[RawBlock],
    page_height: float,
    y_gap_threshold: float = 15.0,
) -> list[list[RawBlock]]:
    """Group text blocks that are vertically close and have overlapping x-ranges.

    Two grouping passes:
      1. Sequential pass by y0 proximity.
      2. Merge pass: fuse groups whose bounding boxes overlap vertically
         and share x-range overlap (handles multi-line table cells).
    """
    if not blocks:
        return []

    sorted_blocks = sorted(blocks, key=lambda b: (b.bbox[1], b.bbox[0]))
    groups: list[list[RawBlock]] = []
    current_group: list[RawBlock] = [sorted_blocks[0]]

    for block in sorted_blocks[1:]:
        prev = current_group[-1]
        y_gap = block.bbox[1] - prev.bbox[3]
        x_overlap = _x_overlap(prev, block)

        # Also allow grouping if blocks are on the same y-line (row-based tables)
        same_line = abs(block.bbox[1] - prev.bbox[1]) < 5.0

        if (y_gap <= y_gap_threshold and x_overlap > 0) or same_line:
            current_group.append(block)
        else:
            if len(current_group) >= 2:
                groups.append(current_group)
            current_group = [block]

    if len(current_group) >= 2:
        groups.append(current_group)

    # Merge pass: combine groups whose bounding boxes are close vertically
    # and share x-range overlap (handles multi-line table cells with small gaps)
    if len(groups) >= 2:
        merged = True
        while merged:
            merged = False
            new_groups: list[list[RawBlock]] = []
            used: set[int] = set()
            for i in range(len(groups)):
                if i in used:
                    continue
                g1 = groups[i]
                for j in range(i + 1, len(groups)):
                    if j in used:
                        continue
                    g2 = groups[j]
                    bbox1 = _group_bbox(g1)
                    bbox2 = _group_bbox(g2)
                    # Allow small gap (5pt) between groups for vertical proximity
                    y_gap = max(0, max(bbox1[1], bbox2[1]) - min(bbox1[3], bbox2[3]))
                    y_close = y_gap <= 5.0
                    x_overlap_g = min(bbox1[2], bbox2[2]) - max(bbox1[0], bbox2[0])
                    if y_close and x_overlap_g > 0:
                        g1 = g1 + g2
                        used.add(j)
                        merged = True
                new_groups.append(g1)
                used.add(i)
            groups = new_groups

    return groups


def _x_overlap(a: RawBlock, b: RawBlock) -> float:
    """Return the width of x-overlap between two blocks (0 if none)."""
    x0 = max(a.bbox[0], b.bbox[0])
    x1 = min(a.bbox[2], b.bbox[2])
    return max(0.0, x1 - x0)


def _table_score(group: list[RawBlock], drawings: list[RawDrawing]) -> float:
    """Score a group of blocks as a potential table."""
    alignment = _column_alignment_score(group)
    repetition = _column_repetition_score(group)
    borders = _border_score(group, drawings)
    density = _uniform_density_score(group)
    return (
        alignment * _TABLE_WEIGHTS["alignment"]
        + repetition * _TABLE_WEIGHTS["repetition"]
        + borders * _TABLE_WEIGHTS["borders"]
        + density * _TABLE_WEIGHTS["density"]
    )


def _column_alignment_score(group: list[RawBlock]) -> float:
    """Do blocks have similar x0 values across multiple columns?

    A true table has 2+ distinct column positions (x0 clusters).
    A paragraph has all blocks at the same x0 → low score.
    """
    x0s = sorted(b.bbox[0] for b in group)
    if len(x0s) < 2:
        return 0.0
    clusters = _cluster_values(x0s, tolerance=10.0)
    if len(clusters) <= 1:
        # All blocks at same x0 → paragraph-like, not a table
        return 0.0
    # 2-3 columns is ideal for a table
    if len(clusters) <= 3:
        return 1.0
    # More distinct columns = slightly lower score
    return max(0.0, 1.0 - (len(clusters) - 3) * 0.15)


def _column_repetition_score(group: list[RawBlock]) -> float:
    """Are there consistent column widths across rows?"""
    # Group blocks into rows by y proximity
    rows = _group_into_rows(group)
    if len(rows) < 2:
        return 0.0

    col_widths_per_row: list[list[float]] = []
    for row in rows:
        widths = sorted(b.bbox[2] - b.bbox[0] for b in row)
        if widths:
            col_widths_per_row.append(widths)

    if len(col_widths_per_row) < 2:
        return 0.0

    # Check consistency of first column width
    first_cols = [w[0] for w in col_widths_per_row if w]
    if not first_cols:
        return 0.0
    mean_w = sum(first_cols) / len(first_cols)
    if mean_w <= 0:
        return 0.0
    variance = sum((w - mean_w) ** 2 for w in first_cols) / len(first_cols)
    cv = (variance ** 0.5) / mean_w
    # Low coefficient of variation = high score
    return max(0.0, 1.0 - cv)


def _border_score(group: list[RawBlock], drawings: list[RawDrawing]) -> float:
    """Count rectangles that intersect the group's bounding box."""
    if not drawings:
        return 0.0
    bbox = _group_bbox(group)
    rects = [d for d in drawings if d.type == "re"]
    if not rects:
        return 0.0
    intersections = sum(1 for r in rects if _rects_intersect(bbox, r.bbox))
    # 1 border → 0.3, 2+ → higher, saturates around 4
    return min(1.0, intersections * 0.3)


def _uniform_density_score(group: list[RawBlock]) -> float:
    """Is text evenly distributed across columns?"""
    rows = _group_into_rows(group)
    if len(rows) < 2:
        return 0.0
    cells_per_row = [len(row) for row in rows]
    if not cells_per_row:
        return 0.0
    mean_cells = sum(cells_per_row) / len(cells_per_row)
    if mean_cells <= 0:
        return 0.0
    variance = sum((c - mean_cells) ** 2 for c in cells_per_row) / len(cells_per_row)
    cv = (variance ** 0.5) / mean_cells
    return max(0.0, 1.0 - cv)


def _group_bbox(group: list[RawBlock]) -> list[float]:
    """Compute the bounding box of all blocks in the group."""
    x0 = min(b.bbox[0] for b in group)
    y0 = min(b.bbox[1] for b in group)
    x1 = max(b.bbox[2] for b in group)
    y1 = max(b.bbox[3] for b in group)
    return [x0, y0, x1, y1]


def _rects_intersect(a: list[float], b: list[float]) -> bool:
    """Check if two bounding boxes intersect."""
    return (
        a[0] < b[2]
        and a[2] > b[0]
        and a[1] < b[3]
        and a[3] > b[1]
    )


def _group_into_rows(blocks: list[RawBlock], y_tolerance: float = 5.0) -> list[list[RawBlock]]:
    """Group blocks into rows based on y-center proximity."""
    sorted_blocks = sorted(blocks, key=lambda b: (b.bbox[1], b.bbox[0]))
    rows: list[list[RawBlock]] = []
    current_row: list[RawBlock] = [sorted_blocks[0]]
    current_y = (sorted_blocks[0].bbox[1] + sorted_blocks[0].bbox[3]) / 2

    for block in sorted_blocks[1:]:
        block_y = (block.bbox[1] + block.bbox[3]) / 2
        if abs(block_y - current_y) <= y_tolerance:
            current_row.append(block)
        else:
            rows.append(sorted(current_row, key=lambda b: b.bbox[0]))
            current_row = [block]
            current_y = block_y

    rows.append(sorted(current_row, key=lambda b: b.bbox[0]))
    return rows


def _cluster_values(values: list[float], tolerance: float = 5.0) -> list[list[float]]:
    """Cluster a sorted list of values into groups within tolerance."""
    if not values:
        return []
    clusters: list[list[float]] = [[values[0]]]
    for v in values[1:]:
        if v - clusters[-1][-1] <= tolerance:
            clusters[-1].append(v)
        else:
            clusters.append([v])
    return clusters


def _build_table_element(
    group: list[RawBlock],
    score: float,
    page_num: int,
    elem_idx: int,
) -> DocumentElement:
    """Build a DocumentElement for a confirmed table."""
    sorted_blocks = sorted(group, key=lambda b: (b.bbox[1], b.bbox[0]))
    rows = _group_into_rows(sorted_blocks)

    # First row as headers
    headers = [b.text.strip() for b in rows[0]] if rows else []
    body: list[list[str]] = (
        [[b.text.strip() for b in row] for row in rows[1:]] if len(rows) > 1 else []
    )

    bbox = _group_bbox(group)
    return DocumentElement(
        id=f"p{page_num}-tbl{elem_idx:03d}",
        type=ElementType.TABLE,
        text="\n".join(b.text.strip() for b in sorted_blocks),
        bbox=bbox,
        page=page_num,
        confidence=min(score, 1.0),
        table_data=TableData(headers=headers, rows=body),
    )


# --- Remaining block classification ---


def _classify_remaining_blocks(
    blocks: list[RawBlock],
    heading_ids: set[str],
    table_ids: set[str],
    page_num: int,
) -> list[DocumentElement]:
    """Classify blocks that aren't headings or tables."""
    elements: list[DocumentElement] = []
    idx = 0
    for block in blocks:
        if block.id in heading_ids or block.id in table_ids:
            continue
        if block.block_type != 0:
            continue

        text = block.text.strip()
        if not text:
            continue

        elem_type = _infer_element_type(block)
        elements.append(DocumentElement(
            id=f"p{page_num}-e{idx:03d}",
            type=elem_type,
            text=text,
            bbox=list(block.bbox),
            page=page_num,
            confidence=0.9 if elem_type != ElementType.PARAGRAPH else 1.0,
        ))
        idx += 1

    return elements


def _create_figure_elements(
    page: RawPageModel,
    page_num: int,
    global_img_idx: int,
) -> tuple[list[DocumentElement], list[ImageRef], int]:
    """Crée un FIGURE element + ImageRef par image embarquée de la page.

    Returns:
        (figure_elements, image_refs, next_global_img_idx)
    """
    elements: list[DocumentElement] = []
    refs: list[ImageRef] = []

    for local_idx, image in enumerate(page.images):
        placeholder_id = f"img_{global_img_idx + 1:03d}"
        elements.append(DocumentElement(
            id=f"p{page_num}-f{local_idx:03d}",
            type=ElementType.FIGURE,
            text="",
            bbox=list(image.bbox),
            page=page_num,
            confidence=1.0,
            image_id=placeholder_id,
        ))
        refs.append(ImageRef(
            placeholder_id=placeholder_id,
            content_type=image.content_type,
            bbox=list(image.bbox),
            page=page_num,
        ))
        global_img_idx += 1

    return elements, refs, global_img_idx


def _infer_element_type(block: RawBlock) -> ElementType:
    """Infer the element type for a non-heading, non-table text block."""
    if _detect_list_items(block):
        return ElementType.LIST
    if block.text.strip().startswith("> "):
        return ElementType.QUOTE
    if _is_monospace(block):
        return ElementType.CODE
    return ElementType.PARAGRAPH


def _detect_list_items(block: RawBlock) -> bool:
    """Check if block contains list items."""
    first_line = block.text.split("\n", 1)[0].strip()
    if _LIST_BULLET_RE.match(first_line):
        return True
    if _NUMBERED_LIST_RE.match(first_line):
        return True
    return False


def _is_monospace(block: RawBlock) -> bool:
    """Check if all fonts in the block are monospace."""
    if not block.fonts:
        return False
    for font in block.fonts:
        base = font.split("-")[0].split("+")[-1].lower().replace(" ", "")
        if base not in _MONOSPACE_FONTS:
            return False
    return True
