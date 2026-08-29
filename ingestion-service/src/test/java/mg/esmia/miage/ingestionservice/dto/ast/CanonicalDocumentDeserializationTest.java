package mg.esmia.miage.ingestionservice.dto.ast;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.esmia.miage.ingestionservice.service.docker.DoclingConversionResult;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste la désérialisation Jackson du JSON Python (CanonicalDocument AST)
 * vers les records Java. C'est le contrat qui doit être verrouillé avant
 * le StructureAwareChunker.
 */
class CanonicalDocumentDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sampleAstDeserializesCorrectly() throws Exception {
        InputStream is = getClass().getResourceAsStream("/fixtures/sample_ast.json");
        assertNotNull(is, "Fixture JSON introuvable");

        CanonicalDocument doc = objectMapper.readValue(is, CanonicalDocument.class);

        // Structure de base
        assertNotNull(doc);
        assertEquals(2, doc.pages().size());
        assertTrue(doc.images().isEmpty());

        // Page 1
        PageAST page1 = doc.pages().get(0);
        assertEquals(1, page1.page());
        assertEquals(11, page1.elements().size());

        // --- Heading ---
        DocumentElement heading = page1.elements().get(0);
        assertEquals("p1-h000", heading.id());
        assertEquals(ElementType.HEADING, heading.type());
        assertEquals(1, heading.level());
        assertEquals("Chapitre 1", heading.text());
        assertNotNull(heading.bbox());
        assertEquals(4, heading.bbox().size());
        assertEquals(72.0f, heading.bbox().get(0));
        assertNull(heading.parentId());
        assertEquals(0.875, heading.confidence(), 0.001);
        assertNull(heading.tableData());

        // --- Paragraph ---
        DocumentElement para = page1.elements().get(1);
        assertEquals("p1-e000", para.id());
        assertEquals(ElementType.PARAGRAPH, para.type());
        assertNull(para.level());
        assertEquals("Contenu du paragraphe.", para.text());
        assertEquals("p1-h000", para.parentId());
        assertEquals(1.0, para.confidence(), 0.001);
        assertNull(para.tableData());

        // --- Paragraph 2 ---
        DocumentElement para2 = page1.elements().get(2);
        assertEquals(ElementType.PARAGRAPH, para2.type());
        assertEquals("p1-h000", para2.parentId());

        // --- Heading H2 ---
        DocumentElement h2 = page1.elements().get(3);
        assertEquals(ElementType.HEADING, h2.type());
        assertEquals(2, h2.level());
        assertEquals("Section 1.1", h2.text());
        assertEquals("p1-h000", h2.parentId());

        // --- Paragraph sous H2 ---
        DocumentElement h2Para = page1.elements().get(4);
        assertEquals("p1-h001", h2Para.parentId());

        // --- Table ---
        DocumentElement table = page1.elements().get(5);
        assertEquals("p1-tbl000", table.id());
        assertEquals(ElementType.TABLE, table.type());
        assertNotNull(table.tableData());
        List<String> headers = table.tableData().headers();
        assertEquals(2, headers.size());
        assertEquals("Nom", headers.get(0));
        assertEquals("Ville", headers.get(1));
        List<List<String>> rows = table.tableData().rows();
        assertEquals(2, rows.size());
        assertEquals(List.of("Alice", "Paris"), rows.get(0));
        assertEquals(List.of("Bob", "Londres"), rows.get(1));

        // --- List ---
        DocumentElement list = page1.elements().get(6);
        assertEquals(ElementType.LIST, list.type());
        assertEquals(0.85, list.confidence(), 0.001);

        // --- Figure ---
        DocumentElement figure = page1.elements().get(7);
        assertEquals(ElementType.FIGURE, figure.type());
        assertEquals("p1-e004", figure.id());
        assertEquals("img_001", figure.imageId());

        // --- Caption ---
        DocumentElement caption = page1.elements().get(8);
        assertEquals(ElementType.CAPTION, caption.type());
        assertEquals("p1-e004", caption.parentId());

        // --- Code ---
        DocumentElement code = page1.elements().get(9);
        assertEquals(ElementType.CODE, code.type());
        assertEquals("def hello():\n    print('world')", code.text());

        // --- Quote ---
        DocumentElement quote = page1.elements().get(10);
        assertEquals(ElementType.QUOTE, quote.type());
        assertEquals("p1-h000", quote.parentId());

        // Page 2
        PageAST page2 = doc.pages().get(1);
        assertEquals(2, page2.page());
        assertEquals(2, page2.elements().size());
        assertEquals("Chapitre 2", page2.elements().get(0).text());
        assertEquals("p2-h000", page2.elements().get(0).id());
    }

    @Test
    void nullFieldsDeserializeAsNull() throws Exception {
        String json = """
            {"pages": [{"page": 1, "elements": [
                {"id": "x", "type": "paragraph", "level": null,
                 "text": "t", "bbox": [0,0,0,0], "page": 1,
                 "parent_id": null, "confidence": 1.0, "table_data": null}
            ]}], "images": []}
            """;
        CanonicalDocument doc = objectMapper.readValue(json, CanonicalDocument.class);
        DocumentElement e = doc.pages().get(0).elements().get(0);
        assertNull(e.level());
        assertNull(e.parentId());
        assertNull(e.tableData());
    }

    @Test
    void unknownElementTypeThrows() {
        String json = """
            {"pages": [{"page": 1, "elements": [
                {"id": "x", "type": "unknown_type", "text": "t",
                 "bbox": [0,0,0,0], "page": 1, "confidence": 1.0}
            ]}], "images": []}
            """;
        assertThrows(Exception.class, () ->
            objectMapper.readValue(json, CanonicalDocument.class));
    }

    @Test
    void elementTypeCaseInsensitive() throws Exception {
        String json = """
            {"pages": [{"page": 1, "elements": [
                {"id": "x", "type": "HEADING", "text": "t",
                 "bbox": [0,0,0,0], "page": 1, "confidence": 1.0}
            ]}], "images": []}
            """;
        CanonicalDocument doc = objectMapper.readValue(json, CanonicalDocument.class);
        assertEquals(ElementType.HEADING, doc.pages().get(0).elements().get(0).type());
    }

    @Test
    void fullDoclingConversionResultDeserializes() throws Exception {
        String json = """
            {
              "document": {
                "pages": [{"page": 1, "elements": [
                  {"id": "h1", "type": "heading", "level": 1, "text": "Titre",
                   "bbox": [0,0,100,20], "page": 1, "parent_id": null,
                   "confidence": 0.9, "table_data": null}
                ]}],
                "images": []
              },
              "markdown": "# Titre\\n\\nContenu.",
              "method": "pymupdf_layout",
              "pages_processed": 1,
              "warnings": [],
              "images": []
            }
            """;
        DoclingConversionResult result = objectMapper.readValue(json, DoclingConversionResult.class);

        assertNotNull(result.document());
        assertEquals("pymupdf_layout", result.method());
        assertEquals(1, result.pagesProcessed());
        assertTrue(result.warnings().isEmpty());
        assertTrue(result.images().isEmpty());

        CanonicalDocument doc = result.document();
        assertEquals(1, doc.pages().size());
        assertEquals("Titre", doc.pages().get(0).elements().get(0).text());
    }

    @Test
    void nonPdfDocumentHasNullDocument() throws Exception {
        String json = """
            {
              "document": null,
              "markdown": "# Rapport\\n\\nTexte.",
              "method": "markitdown",
              "pages_processed": 1,
              "warnings": [],
              "images": []
            }
            """;
        DoclingConversionResult result = objectMapper.readValue(json, DoclingConversionResult.class);
        assertNull(result.document());
        assertEquals("markitdown", result.method());
    }

    @Test
    void pageWithNoElementsDeserializes() throws Exception {
        String json = """
            {"pages": [{"page": 3, "elements": []}], "images": []}
            """;
        CanonicalDocument doc = objectMapper.readValue(json, CanonicalDocument.class);
        assertEquals(1, doc.pages().size());
        assertEquals(3, doc.pages().get(0).page());
        assertTrue(doc.pages().get(0).elements().isEmpty());
    }
}
