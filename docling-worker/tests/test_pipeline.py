"""Tests du nouveau pipeline PDF : layout extraction → classification → AST → Markdown."""
import io
import pytest
import fitz  # PyMuPDF

from app.models import (
    RawPageModel, RawBlock, RawLine, RawSpan, RawImage, RawDrawing,
    PageClassification, PageType, CanonicalDocument, DocumentElement,
    ElementType, TableData, PageAST,
)
from app.layout_extractor import extract_raw_document
from app.page_classifier import classify_page, classify_document
from app.structure_analyzer import StructureAnalyzer
from app.markdown_renderer import render
from app.markitdown_converter import DocumentConverter


# --- Helpers to create synthetic PDFs ---

def _create_text_pdf(text: str, font_size: int = 12, bold: bool = False) -> bytes:
    """Create a simple PDF with one page containing text."""
    doc = fitz.open()
    page = doc.new_page()
    fontname = "helv"  # Helvetica
    if bold:
        fontname = "hebo"  # Helvetica Bold
    page.insert_text((72, 100), text, fontsize=font_size, fontname=fontname)
    doc_bytes = doc.tobytes()
    doc.close()
    return doc_bytes


def _create_multi_heading_pdf() -> bytes:
    """Create a PDF with H1, H2, H3 headings and paragraphs."""
    doc = fitz.open()
    page = doc.new_page()

    # H1 — large bold
    page.insert_text((72, 80), "Chapter 1: Introduction", fontsize=18, fontname="hebo")
    # Paragraph — small regular
    page.insert_text((72, 120), "This is a paragraph with some text content.", fontsize=10, fontname="helv")
    # H2 — medium bold
    page.insert_text((72, 160), "1.1 Background", fontsize=14, fontname="hebo")
    # Paragraph
    page.insert_text((72, 200), "More paragraph text here.", fontsize=10, fontname="helv")
    # H3 — smaller bold
    page.insert_text((72, 240), "1.1.1 Details", fontsize=12, fontname="hebo")
    # Paragraph
    page.insert_text((72, 280), "Even more text.", fontsize=10, fontname="helv")

    doc_bytes = doc.tobytes()
    doc.close()
    return doc_bytes


def _create_table_like_pdf() -> bytes:
    """Create a PDF with text arranged in a table-like layout."""
    doc = fitz.open()
    page = doc.new_page()

    # Headers (bold)
    page.insert_text((72, 80), "Name", fontsize=10, fontname="hebo")
    page.insert_text((200, 80), "Age", fontsize=10, fontname="hebo")
    page.insert_text((320, 80), "City", fontsize=10, fontname="hebo")
    # Row 1
    page.insert_text((72, 100), "Alice", fontsize=10, fontname="helv")
    page.insert_text((200, 100), "25", fontsize=10, fontname="helv")
    page.insert_text((320, 100), "Paris", fontsize=10, fontname="helv")
    # Row 2
    page.insert_text((72, 120), "Bob", fontsize=10, fontname="helv")
    page.insert_text((200, 120), "30", fontsize=10, fontname="helv")
    page.insert_text((320, 120), "London", fontsize=10, fontname="helv")

    doc_bytes = doc.tobytes()
    doc.close()
    return doc_bytes


# ===========================================================================
# 1. Layout extractor tests
# ===========================================================================

class TestLayoutExtractor:
    def test_extract_single_page(self):
        """A single-page PDF produces one RawPageModel."""
        pdf = _create_text_pdf("Hello World")
        pages = extract_raw_document(pdf)
        assert len(pages) == 1
        assert pages[0].page_num == 1
        assert pages[0].width > 0
        assert pages[0].height > 0

    def test_extract_text_blocks(self):
        """Text blocks are extracted with content."""
        pdf = _create_text_pdf("Test content")
        pages = extract_raw_document(pdf)
        assert len(pages[0].blocks) > 0
        assert any("Test content" in b.text for b in pages[0].blocks)

    def test_extract_multi_page(self):
        """A multi-page PDF produces multiple RawPageModels."""
        doc = fitz.open()
        for _ in range(3):
            page = doc.new_page()
            page.insert_text((72, 100), "Page content", fontsize=12, fontname="helv")
        pdf_bytes = doc.tobytes()
        doc.close()
        pages = extract_raw_document(pdf_bytes)
        assert len(pages) == 3

    def test_font_sizes_populated(self):
        """RawBlock.font_sizes is populated from spans."""
        pdf = _create_text_pdf("Test", font_size=14)
        pages = extract_raw_document(pdf)
        block = pages[0].blocks[0]
        assert len(block.font_sizes) > 0
        assert 14 in block.font_sizes or any(13 <= s <= 15 for s in block.font_sizes)

    def test_bold_ratio(self):
        """Bold text has a non-zero bold_ratio."""
        pdf = _create_text_pdf("Bold text", bold=True)
        pages = extract_raw_document(pdf)
        bold_blocks = [b for b in pages[0].blocks if b.bold_ratio > 0]
        assert len(bold_blocks) > 0

    def test_page_dimensions(self):
        """Extracted page has correct A4-like dimensions."""
        pdf = _create_text_pdf("Test")
        pages = extract_raw_document(pdf)
        # Standard A4: ~595 x 842 points
        assert 500 < pages[0].width < 700
        assert 700 < pages[0].height < 950

    def test_block_bbox_populated(self):
        """Blocks have non-zero bounding boxes."""
        pdf = _create_text_pdf("Test bbox")
        pages = extract_raw_document(pdf)
        block = pages[0].blocks[0]
        assert block.bbox[0] >= 0
        assert block.bbox[1] >= 0
        assert block.bbox[2] > block.bbox[0]
        assert block.bbox[3] > block.bbox[1]

    def test_block_has_lines(self):
        """Blocks contain at least one RawLine with spans."""
        pdf = _create_text_pdf("Lines test")
        pages = extract_raw_document(pdf)
        block = pages[0].blocks[0]
        assert len(block.lines) >= 1
        assert len(block.lines[0].spans) >= 1

    def test_span_metadata(self):
        """Spans have font name, size, and flags."""
        pdf = _create_text_pdf("Span metadata")
        pages = extract_raw_document(pdf)
        span = pages[0].blocks[0].lines[0].spans[0]
        assert span.font != ""
        assert span.size > 0
        assert isinstance(span.flags, int)

    def test_empty_pdf_no_crash(self):
        """A PDF with an empty page doesn't crash."""
        doc = fitz.open()
        doc.new_page()
        pdf_bytes = doc.tobytes()
        doc.close()
        pages = extract_raw_document(pdf_bytes)
        assert len(pages) == 1
        # Empty page may have 0 or 1 blocks (depending on PyMuPDF)
        assert isinstance(pages[0].blocks, list)


# ===========================================================================
# 2. Page classifier tests
# ===========================================================================

class TestPageClassifier:
    def test_text_page_is_native(self):
        """A page with text should be classified as native."""
        doc = fitz.open()
        page = doc.new_page()
        for i in range(30):
            page.insert_text((72, 80 + i * 20), "A" * 60, fontsize=10, fontname="helv")
        pdf = doc.tobytes()
        doc.close()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        assert classifications[0].page_type == PageType.NATIVE

    def test_empty_page(self):
        """An empty page is classified as scanned (no text)."""
        doc = fitz.open()
        doc.new_page()
        pdf_bytes = doc.tobytes()
        doc.close()
        pages = extract_raw_document(pdf_bytes)
        classifications = classify_document(pages)
        # Empty page → no text → should be scanned or hybrid
        assert classifications[0].page_type in (PageType.SCANNED, PageType.HYBRID)

    def test_classification_has_signals(self):
        """Classification includes text_density and image_ratio."""
        pdf = _create_text_pdf("Some text")
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        assert classifications[0].text_density >= 0
        assert classifications[0].image_ratio >= 0

    def test_classify_single_page(self):
        """classify_page works on a single RawPageModel."""
        pdf = _create_text_pdf("Test single page classification")
        pages = extract_raw_document(pdf)
        result = classify_page(pages[0])
        assert isinstance(result, PageClassification)
        assert result.page_num == 1

    def test_multi_page_classification(self):
        """Multi-page PDF produces one classification per page."""
        doc = fitz.open()
        for i in range(4):
            page = doc.new_page()
            page.insert_text((72, 100), f"Page {i+1} content", fontsize=12, fontname="helv")
        pdf_bytes = doc.tobytes()
        doc.close()
        pages = extract_raw_document(pdf_bytes)
        classifications = classify_document(pages)
        assert len(classifications) == 4
        for c in classifications:
            assert c.page_type in (PageType.NATIVE, PageType.HYBRID, PageType.SCANNED)

    def test_text_density_positive_for_text(self):
        """Text page has positive text_density."""
        pdf = _create_text_pdf("A" * 300)
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        assert classifications[0].text_density > 0

    def test_block_count_populated(self):
        """Classification includes block_count from the page."""
        pdf = _create_text_pdf("Block count test")
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        assert classifications[0].block_count >= 1

    def test_rich_text_page_is_native(self):
        """A page with substantial text and multiple blocks is native."""
        doc = fitz.open()
        page = doc.new_page()
        for i in range(20):
            page.insert_text((72, 80 + i * 25), f"Line {i} with enough text to fill the page width", fontsize=10, fontname="helv")
        pdf_bytes = doc.tobytes()
        doc.close()
        pages = extract_raw_document(pdf_bytes)
        classifications = classify_document(pages)
        assert classifications[0].page_type == PageType.NATIVE


# ===========================================================================
# 3. Structure analyzer tests
# ===========================================================================

class TestStructureAnalyzer:
    def test_heading_detection(self):
        """Headings are detected based on font size + bold."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)

        headings = [e for e in doc.pages[0].elements if e.type == ElementType.HEADING]
        assert len(headings) >= 2  # At least H1 and H2

    def test_paragraph_detection(self):
        """Non-heading text blocks become paragraphs."""
        pdf = _create_text_pdf("A regular paragraph with enough content to be detected.")
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)

        paragraphs = [e for e in doc.pages[0].elements if e.type == ElementType.PARAGRAPH]
        assert len(paragraphs) >= 1

    def test_heading_hierarchy(self):
        """Headings have parent_id set correctly."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)

        headings = [e for e in doc.pages[0].elements if e.type == ElementType.HEADING]
        if len(headings) >= 2:
            # The second heading should have a parent
            assert headings[1].parent_id is not None

    def test_scanned_page_placeholder(self):
        """Scanned pages get a placeholder element."""
        doc = fitz.open()
        page = doc.new_page()
        page.insert_text((72, 100), "X", fontsize=10, fontname="helv")
        pdf_bytes = doc.tobytes()
        doc.close()

        pages = extract_raw_document(pdf_bytes)
        classifications = classify_document(pages)
        # Force scanned classification for testing
        classifications[0].page_type = PageType.SCANNED

        analyzer = StructureAnalyzer()
        doc_ast = analyzer.analyze(pages, classifications)

        assert len(doc_ast.pages) == 1
        assert len(doc_ast.pages[0].elements) >= 1

    def test_scanned_placeholder_text(self):
        """Scanned page placeholder contains page number."""
        doc = fitz.open()
        doc.new_page()
        pdf_bytes = doc.tobytes()
        doc.close()

        pages = extract_raw_document(pdf_bytes)
        classifications = classify_document(pages)
        classifications[0].page_type = PageType.SCANNED

        analyzer = StructureAnalyzer()
        doc_ast = analyzer.analyze(pages, classifications)

        placeholder = doc_ast.pages[0].elements[0]
        assert "Page 1" in placeholder.text
        assert "scanned" in placeholder.text.lower()

    def test_canonical_document_pages_match(self):
        """CanonicalDocument has one PageAST per raw page."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)
        assert len(doc.pages) == len(pages)

    def test_elements_have_ids(self):
        """All elements have non-empty IDs."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)
        for page_ast in doc.pages:
            for elem in page_ast.elements:
                assert elem.id != ""

    def test_elements_have_bbox(self):
        """All elements have bounding boxes."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)
        for page_ast in doc.pages:
            for elem in page_ast.elements:
                assert len(elem.bbox) == 4

    def test_heading_level_assignment(self):
        """Headings have levels between 1 and 6."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)
        headings = [e for e in doc.pages[0].elements if e.type == ElementType.HEADING]
        for h in headings:
            assert h.level is not None
            assert 1 <= h.level <= 6

    def test_heading_levels_ordered_by_size(self):
        """Larger heading text gets a lower (more important) level number."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)
        headings = [e for e in doc.pages[0].elements if e.type == ElementType.HEADING]
        if len(headings) >= 2:
            # H1 (fontsize 18) should have level < H2 (fontsize 14)
            levels = [h.level for h in headings]
            assert levels[0] <= levels[1]

    def test_heading_confidence(self):
        """Headings have confidence between 0 and 1."""
        pdf = _create_multi_heading_pdf()
        pages = extract_raw_document(pdf)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc = analyzer.analyze(pages, classifications)
        headings = [e for e in doc.pages[0].elements if e.type == ElementType.HEADING]
        for h in headings:
            assert 0.0 <= h.confidence <= 1.0

    def test_empty_page_ast(self):
        """A page with no text blocks produces an empty elements list (or scanned placeholder)."""
        doc = fitz.open()
        doc.new_page()
        pdf_bytes = doc.tobytes()
        doc.close()

        pages = extract_raw_document(pdf_bytes)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc_ast = analyzer.analyze(pages, classifications)

        assert len(doc_ast.pages) == 1
        # Either empty elements or a scanned placeholder
        assert isinstance(doc_ast.pages[0].elements, list)

    def test_multi_page_ast(self):
        """Multi-page PDF produces multi-page AST."""
        doc = fitz.open()
        for i in range(3):
            page = doc.new_page()
            page.insert_text((72, 80), f"Heading {i+1}", fontsize=16, fontname="hebo")
            page.insert_text((72, 120), f"Paragraph text for page {i+1} with enough content.", fontsize=10, fontname="helv")
        pdf_bytes = doc.tobytes()
        doc.close()

        pages = extract_raw_document(pdf_bytes)
        classifications = classify_document(pages)
        analyzer = StructureAnalyzer()
        doc_ast = analyzer.analyze(pages, classifications)
        assert len(doc_ast.pages) == 3


# ===========================================================================
# 4. Markdown renderer tests
# ===========================================================================

class TestMarkdownRenderer:
    def test_render_heading(self):
        """Headings render with correct prefix."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.HEADING, level=1,
                            text="Title", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert "# Title" in md

    def test_render_paragraph(self):
        """Paragraphs render as plain text."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.PARAGRAPH,
                            text="Hello world", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert "Hello world" in md
        # No heading prefix on paragraph text
        lines = [l for l in md.strip().split("\n") if "Hello world" in l]
        assert all(not l.startswith("#") for l in lines)

    def test_render_table(self):
        """Tables render as Markdown tables."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.TABLE, text="table",
                            bbox=[0, 0, 100, 50], page=1,
                            table_data=TableData(headers=["A", "B"], rows=[["1", "2"]])),
        ])])
        md = render(doc)
        assert "| A | B |" in md
        assert "| --- | --- |" in md
        assert "| 1 | 2 |" in md

    def test_render_list(self):
        """Lists render with bullet points."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.LIST,
                            text="item1\nitem2\nitem3", bbox=[0, 0, 100, 50], page=1),
        ])])
        md = render(doc)
        assert "- item1" in md
        assert "- item2" in md

    def test_render_code(self):
        """Code blocks render with fenced syntax."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.CODE,
                            text="print('hello')", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert "```" in md
        assert "print('hello')" in md

    def test_empty_document(self):
        """Empty document produces minimal output."""
        doc = CanonicalDocument(pages=[])
        md = render(doc)
        assert md.strip() == ""

    def test_full_page_render(self):
        """A full page with mixed elements renders correctly."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.HEADING, level=1,
                            text="Chapter 1", bbox=[0, 0, 100, 20], page=1),
            DocumentElement(id="e2", type=ElementType.PARAGRAPH,
                            text="Some text here.", bbox=[0, 30, 100, 50], page=1),
            DocumentElement(id="e3", type=ElementType.HEADING, level=2,
                            text="Section 1.1", bbox=[0, 60, 100, 80], page=1),
            DocumentElement(id="e4", type=ElementType.PARAGRAPH,
                            text="More text.", bbox=[0, 90, 100, 110], page=1),
        ])])
        md = render(doc)
        assert "# Chapter 1" in md
        assert "## Section 1.1" in md
        assert "Some text here." in md
        assert "More text." in md

    def test_heading_level2(self):
        """H2 headings render with ## prefix."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.HEADING, level=2,
                            text="Subsection", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert md.startswith("## Subsection")

    def test_heading_level3(self):
        """H3 headings render with ### prefix."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.HEADING, level=3,
                            text="Sub-subsection", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert "### Sub-subsection" in md

    def test_heading_empty_text(self):
        """Empty heading text produces no output."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.HEADING, level=1,
                            text="", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert md.strip() == ""

    def test_paragraph_empty_text(self):
        """Empty paragraph text produces no output."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.PARAGRAPH,
                            text="", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert md.strip() == ""

    def test_render_quote(self):
        """Quotes render with > prefix."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.QUOTE,
                            text="quoted text", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert "> quoted text" in md

    def test_render_caption(self):
        """Captions render in italic."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.CAPTION,
                            text="Figure 1", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert "*Figure 1*" in md

    def test_render_figure_with_image_id(self):
        """Figures with image_id render with correct placeholder."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.FIGURE,
                            text="A diagram", bbox=[0, 0, 100, 50], page=1,
                            image_id="img_042"),
        ])])
        md = render(doc)
        assert "{IMAGE:img_042}" in md
        assert "A diagram" in md

    def test_render_figure_without_image_id_raises(self):
        """Figures without image_id raise ValueError."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.FIGURE,
                            text="A diagram", bbox=[0, 0, 100, 50], page=1),
        ])])
        with pytest.raises(ValueError, match="sans image_id"):
            render(doc)

    def test_table_without_data(self):
        """Table without table_data renders as plain text."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.TABLE, text="raw table text",
                            bbox=[0, 0, 100, 50], page=1),
        ])])
        md = render(doc)
        assert "raw table text" in md

    def test_list_with_existing_bullets(self):
        """List items that already have bullets are preserved."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.LIST,
                            text="- already bulleted\n* also bulleted", bbox=[0, 0, 100, 50], page=1),
        ])])
        md = render(doc)
        assert "- already bulleted" in md
        assert "* also bulleted" in md

    def test_multi_page_render(self):
        """Elements from multiple pages all appear in output."""
        doc = CanonicalDocument(pages=[
            PageAST(page=1, elements=[
                DocumentElement(id="p1-e0", type=ElementType.PARAGRAPH,
                                text="Page 1 text", bbox=[0, 0, 100, 20], page=1),
            ]),
            PageAST(page=2, elements=[
                DocumentElement(id="p2-e0", type=ElementType.PARAGRAPH,
                                text="Page 2 text", bbox=[0, 0, 100, 20], page=2),
            ]),
        ])
        md = render(doc)
        assert "Page 1 text" in md
        assert "Page 2 text" in md

    def test_render_output_ends_with_newline(self):
        """Rendered markdown ends with a newline."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.PARAGRAPH,
                            text="Text", bbox=[0, 0, 100, 20], page=1),
        ])])
        md = render(doc)
        assert md.endswith("\n")

    def test_no_excessive_blank_lines(self):
        """Output has at most double newlines (no triple+)."""
        doc = CanonicalDocument(pages=[PageAST(page=1, elements=[
            DocumentElement(id="e1", type=ElementType.PARAGRAPH,
                            text="A", bbox=[0, 0, 100, 20], page=1),
            DocumentElement(id="e2", type=ElementType.PARAGRAPH,
                            text="B", bbox=[0, 30, 100, 50], page=1),
        ])])
        md = render(doc)
        assert "\n\n\n" not in md


# ===========================================================================
# 5. Integration tests (DocumentConverter)
# ===========================================================================

class TestDocumentConverter:
    def test_pdf_returns_document(self):
        """PDF conversion returns an AST in the 'document' field."""
        converter = DocumentConverter()
        pdf = _create_multi_heading_pdf()
        result = converter.convert(pdf, "test.pdf")
        assert "document" in result
        assert result["document"] is not None
        assert "pages" in result["document"]

    def test_pdf_method(self):
        """PDF conversion uses pymupdf_layout method."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Test content")
        result = converter.convert(pdf, "test.pdf")
        assert result["method"] == "pymupdf_layout"

    def test_pdf_has_markdown(self):
        """PDF conversion produces markdown."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Test content for markdown generation")
        result = converter.convert(pdf, "test.pdf")
        assert len(result["markdown"]) > 0

    def test_pdf_pages_processed(self):
        """PDF pages_processed matches actual page count."""
        converter = DocumentConverter()
        doc = fitz.open()
        for _ in range(5):
            page = doc.new_page()
            page.insert_text((72, 100), "Content", fontsize=12, fontname="helv")
        pdf_bytes = doc.tobytes()
        doc.close()
        result = converter.convert(pdf_bytes, "test.pdf")
        assert result["pages_processed"] == 5

    def test_warnings_list(self):
        """Warnings is always a list."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Test")
        result = converter.convert(pdf, "test.pdf")
        assert isinstance(result["warnings"], list)

    def test_pdf_document_has_pages_key(self):
        """The document dict contains a 'pages' list."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Test")
        result = converter.convert(pdf, "test.pdf")
        assert isinstance(result["document"]["pages"], list)

    def test_pdf_document_pages_have_elements(self):
        """Each page in the document has an 'elements' list."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Test elements")
        result = converter.convert(pdf, "test.pdf")
        for page in result["document"]["pages"]:
            assert "elements" in page
            assert isinstance(page["elements"], list)

    def test_pdf_images_is_list(self):
        """Images field is always a list."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Test")
        result = converter.convert(pdf, "test.pdf")
        assert isinstance(result["images"], list)

    def test_pdf_markdown_contains_text(self):
        """Markdown output contains the actual text from the PDF."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("UniqueMarker12345")
        result = converter.convert(pdf, "test.pdf")
        assert "UniqueMarker12345" in result["markdown"]

    def test_pdf_multi_page_markdown(self):
        """Multi-page PDF produces markdown with content from all pages."""
        converter = DocumentConverter()
        doc = fitz.open()
        for i in range(3):
            page = doc.new_page()
            page.insert_text((72, 80), f"UniquePage{i+1}Heading", fontsize=16, fontname="hebo")
            page.insert_text((72, 120), f"UniquePage{i+1}Paragraph content here.", fontsize=10, fontname="helv")
        pdf_bytes = doc.tobytes()
        doc.close()
        result = converter.convert(pdf_bytes, "test.pdf")
        assert "UniquePage1" in result["markdown"]
        assert "UniquePage3" in result["markdown"]

    def test_pdf_structure_document_type(self):
        """Document field is a dict (model_dump output)."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Structure test")
        result = converter.convert(pdf, "test.pdf")
        assert isinstance(result["document"], dict)

    def test_single_page_pdf(self):
        """Single-page PDF is processed correctly end to end."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("Single page end to end test")
        result = converter.convert(pdf, "test.pdf")
        assert result["pages_processed"] == 1
        assert len(result["document"]["pages"]) == 1

    def test_heading_in_markdown(self):
        """Bold large text in PDF appears as heading in markdown."""
        converter = DocumentConverter()
        pdf = _create_text_pdf("My Important Title", font_size=20, bold=True)
        result = converter.convert(pdf, "test.pdf")
        # The title should appear somewhere in the markdown
        assert "My Important Title" in result["markdown"]
