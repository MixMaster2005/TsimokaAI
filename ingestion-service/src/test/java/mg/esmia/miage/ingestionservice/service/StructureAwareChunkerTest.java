package mg.esmia.miage.ingestionservice.service;

import mg.esmia.miage.ingestionservice.dto.StructuredChunk;
import mg.esmia.miage.ingestionservice.dto.ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du StructureAwareChunker.
 *
 * <p>Valide les invariants :
 * <ul>
 *   <li>heading = contexte, pas contenu</li>
 *   <li>parent_id utilisé pour les paths</li>
 *   <li>préambule = headingPath []</li>
 *   <li>TABLE atomique (jamais cassée)</li>
 *   <li>CODE atomique (jamais cassé)</li>
 *   <li>merge respecte headingPath (pas cross-section)</li>
 * </ul>
 */
class StructureAwareChunkerTest {

    private final StructureAwareChunker chunker = new StructureAwareChunker();

    private static final int MAX_CHUNK_CHARS = 1500;

    // -----------------------------------------------------------------------
    // Helpers de construction
    // -----------------------------------------------------------------------

    private DocumentElement heading(String id, int level, String text,
                                     String parentId, int page) {
        return new DocumentElement(id, ElementType.HEADING, level, text,
                List.of(0f, 0f, 100f, 20f), page, parentId, 0.9, null, null);
    }

    private DocumentElement paragraph(String id, String text,
                                      String parentId, int page) {
        return new DocumentElement(id, ElementType.PARAGRAPH, null, text,
                List.of(0f, 0f, 100f, 20f), page, parentId, 1.0, null, null);
    }

    private DocumentElement table(String id, String text, int page) {
        return new DocumentElement(id, ElementType.TABLE, null, text,
                List.of(0f, 0f, 100f, 20f), page, null, 0.8,
                new TableData(List.of("A", "B"), List.of(List.of("1", "2"))), null);
    }

    private DocumentElement code(String id, String text, int page) {
        return new DocumentElement(id, ElementType.CODE, null, text,
                List.of(0f, 0f, 100f, 20f), page, null, 0.95, null, null);
    }

    private DocumentElement list(String id, String text,
                                 String parentId, int page) {
        return new DocumentElement(id, ElementType.LIST, null, text,
                List.of(0f, 0f, 100f, 20f), page, parentId, 0.85, null, null);
    }

    private DocumentElement quote(String id, String text,
                                  String parentId, int page) {
        return new DocumentElement(id, ElementType.QUOTE, null, text,
                List.of(0f, 0f, 100f, 20f), page, parentId, 0.8, null, null);
    }

    private DocumentElement caption(String id, String text, int page) {
        return new DocumentElement(id, ElementType.CAPTION, null, text,
                List.of(0f, 0f, 100f, 20f), page, null, 1.0, null, null);
    }

    private DocumentElement figure(String id, String text, int page) {
        return figure(id, text, page, null);
    }

    private DocumentElement figure(String id, String text, int page, String imageId) {
        return new DocumentElement(id, ElementType.FIGURE, null, text,
                List.of(0f, 0f, 100f, 20f), page, null, 1.0, null, imageId);
    }

    private CanonicalDocument doc(PageAST... pages) {
        return new CanonicalDocument(List.of(pages), List.of());
    }

    private PageAST page(int num, DocumentElement... elements) {
        return new PageAST(num, List.of(elements));
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void emptyDocumentReturnsNoChunks() {
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk(new CanonicalDocument(List.of(), List.of())).isEmpty());
        assertTrue(chunker.chunk(doc(page(1))).isEmpty());
    }

    @Test
    void singleParagraphSingleChunk() {
        CanonicalDocument document = doc(
                page(1, paragraph("p1", "Contenu du paragraphe.", null, 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(1, chunks.size());
        assertEquals("Contenu du paragraphe.", chunks.get(0).text());
        assertEquals(List.of(), chunks.get(0).headingPath());
        assertEquals(1, chunks.get(0).pageStart());
        assertEquals(1, chunks.get(0).pageEnd());
        assertEquals(Set.of(ElementType.PARAGRAPH), chunks.get(0).elementTypes());
        assertTrue(chunks.get(0).imageIds().isEmpty());
    }

    @Test
    void headingAloneDoesNotCreateChunk() {
        CanonicalDocument document = doc(
                page(1, heading("h1", 1, "Titre", null, 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertTrue(chunks.isEmpty());
    }

    @Test
    void headingAndParagraphCreateChunk() {
        CanonicalDocument document = doc(
                page(1,
                        heading("h1", 1, "Titre", null, 1),
                        paragraph("p1", "Contenu sous le titre.", "h1", 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(1, chunks.size());
        // Le heading est désormais préfixé dans le contenu embedé (recherchable)
        assertEquals("Titre\n\nContenu sous le titre.", chunks.get(0).text());
        assertEquals(List.of("Titre"), chunks.get(0).headingPath());
    }

    @Test
    void headingPathBuiltFromParentId() {
        CanonicalDocument document = doc(
                page(1,
                        heading("h1", 1, "Chapitre 1", null, 1),
                        heading("h2", 2, "Section 1.1", "h1", 1),
                        paragraph("p1", "Contenu de la section.", "h2", 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(1, chunks.size());
        assertEquals(List.of("Chapitre 1", "Section 1.1"), chunks.get(0).headingPath());
    }

    @Test
    void headingPathUpdatesAcrossSections() {
        CanonicalDocument document = doc(
                page(1,
                        heading("h1a", 1, "Chapitre A", null, 1),
                        heading("h2a", 2, "Section A.1", "h1a", 1),
                        paragraph("p1", "Contenu A.1", "h2a", 1)),
                page(2,
                        heading("h1b", 1, "Chapitre B", null, 2),
                        paragraph("p2", "Contenu B", "h1b", 2)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(2, chunks.size());
        assertEquals(List.of("Chapitre A", "Section A.1"), chunks.get(0).headingPath());
        assertEquals(List.of("Chapitre B"), chunks.get(1).headingPath());
    }

    @Test
    void tableIsNeverSplit() {
        // Table > MAX_CHUNK_CHARS doit rester intacte
        String tableText = "Col1 | Col2\n" + "------ | -----\n"
                + "a".repeat(MAX_CHUNK_CHARS);
        CanonicalDocument document = doc(
                page(1, table("t1", tableText, 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).text().length() > MAX_CHUNK_CHARS);
        assertEquals(Set.of(ElementType.TABLE), chunks.get(0).elementTypes());
    }

    @Test
    void codeIsNeverSplit() {
        String codeText = "function example() {\n" + "x".repeat(MAX_CHUNK_CHARS) + "\n}";
        CanonicalDocument document = doc(
                page(1, code("c1", codeText, 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).text().length() > MAX_CHUNK_CHARS);
        assertEquals(Set.of(ElementType.CODE), chunks.get(0).elementTypes());
    }

    @Test
    void pageStartEndReflectActualPages() {
        CanonicalDocument document = doc(
                page(3, paragraph("p1", "Contenu page 3", null, 3)),
                page(4, paragraph("p2", "Contenu page 4", null, 4)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).pageStart());
        assertEquals(4, chunks.get(0).pageEnd());
    }

    @Test
    void tinyChunkMergedOnlyWithinSameSection() {
        // Petit chunk sous H1A, puis chunk normal sous H1A → même section → fusionné
        CanonicalDocument document = doc(
                page(1,
                        heading("h1", 1, "Titre", null, 1),
                        paragraph("p1", "x", "h1", 1),
                        paragraph("p2", "a".repeat(200), "h1", 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        // Le petit "x" doit être fusionné avec le paragraphe suivant (même section)
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).text().contains("x"));
    }

    @Test
    void tinyChunkNotMergedCrossSection() {
        // Petit chunk sous H1A, chunk normal sous H1B → sections différentes → pas de fusion
        CanonicalDocument document = doc(
                page(1,
                        heading("h1a", 1, "Chapitre A", null, 1),
                        paragraph("p1", "x", "h1a", 1)),
                page(2,
                        heading("h1b", 1, "Chapitre B", null, 2),
                        paragraph("p2", "a".repeat(200), "h1b", 2)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        // Deux chunks distincts malgré la petitesse du premier
        assertEquals(2, chunks.size());
        // Le heading est désormais préfixé dans le contenu embedé (recherchable)
        assertEquals("Chapitre A\n\nx", chunks.get(0).text());
        assertEquals(List.of("Chapitre A"), chunks.get(0).headingPath());
        assertEquals(List.of("Chapitre B"), chunks.get(1).headingPath());
    }

    @Test
    void mixedContentRespectsTargetSize() {
        // Plusieurs paragraphes → chunks proches de MAX_CHUNK_CHARS
        CanonicalDocument document = doc(
                page(1,
                        paragraph("p1", "a".repeat(800), null, 1),
                        paragraph("p2", "b".repeat(800), null, 1),
                        paragraph("p3", "c".repeat(800), null, 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertFalse(chunks.isEmpty());
        // Au moins 2 chunks car 3 × 800 = 2400 > 1500
        assertTrue(chunks.size() >= 2, "Devrait produire au moins 2 chunks : " + chunks.size());
        for (StructuredChunk chunk : chunks) {
            assertFalse(chunk.text().isBlank());
        }
    }

    @Test
    void elementTypesAreTracked() {
        CanonicalDocument document = doc(
                page(1,
                        heading("h1", 1, "Titre", null, 1),
                        paragraph("p1", "Paragraphe.", "h1", 1),
                        table("t1", "data", 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        // Le paragraphe (petit) est fusionné avec la table car ils partagent le
        // même headingPath ["Titre"] ; le chunk unique cumule les deux types.
        assertEquals(1, chunks.size());
        assertEquals(Set.of(ElementType.PARAGRAPH, ElementType.TABLE), chunks.get(0).elementTypes());
        assertEquals(List.of("Titre"), chunks.get(0).headingPath());
    }

    @Test
    void imageIdsCollectedFromFigureElements() {
        // Figure AVEC image_id → imageIds doit contenir l'ID
        CanonicalDocument withImageId = doc(
                page(1,
                        figure("f1", "Figure 1", 1, "img_001"),
                        caption("c1", "Légende", 1)));
        List<StructuredChunk> chunksWithId = chunker.chunk(withImageId);
        assertFalse(chunksWithId.isEmpty());
        assertEquals(List.of("img_001"), chunksWithId.get(0).imageIds());

        // Figure SANS image_id → imageIds reste vide
        CanonicalDocument withoutImageId = doc(
                page(1,
                        figure("f2", "Figure 2", 1),
                        caption("c2", "Légende", 1)));
        List<StructuredChunk> chunksWithoutId = chunker.chunk(withoutImageId);
        assertFalse(chunksWithoutId.isEmpty());
        assertTrue(chunksWithoutId.get(0).imageIds().isEmpty());
    }

    @Test
    void preambleHasEmptyHeadingPath() {
        CanonicalDocument document = doc(
                page(1,
                        paragraph("p1", "Contenu avant tout titre.", null, 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        assertEquals(1, chunks.size());
        assertEquals(List.of(), chunks.get(0).headingPath());
    }

    @Test
    void chunksAreIndexedSequentially() {
        CanonicalDocument document = doc(
                page(1,
                        heading("h1", 1, "Titre", null, 1),
                        paragraph("p1", "Premier.", "h1", 1),
                        paragraph("p2", "Deuxième.", "h1", 1)));

        List<StructuredChunk> chunks = chunker.chunk(document);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).chunkIndex());
        }
    }
}
