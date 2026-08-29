package mg.esmia.miage.ingestionservice.service;

import mg.esmia.miage.ingestionservice.dto.StructuredChunk;
import mg.esmia.miage.ingestionservice.dto.ast.ElementType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du MarkdownFallbackChunker.
 *
 * <p>Valide que le fallback Markdown produit des StructuredChunk
 * avec les mêmes invariants que le StructureAwareChunker :
 * <ul>
 *   <li>headingPath construit à partir des titres Markdown</li>
 *   <li>blocs code atomiques</li>
 *   <li>merge des petits chunks dans la même section</li>
 *   <li>pageStart/pageEnd = 1 pour non-PDF</li>
 *   <li>indexation séquentielle</li>
 * </ul>
 */
class MarkdownFallbackChunkerTest {

    private final MarkdownFallbackChunker chunker = new MarkdownFallbackChunker();

    @Test
    void videOuNullDonneAucunChunk() {
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   \n  \n").isEmpty());
    }

    @Test
    void petitDocumentSansTitreEstUnSeulChunk() {
        String markdown = "Un paragraphe de contenu, court, sans structure de titres.";
        List<StructuredChunk> chunks = chunker.chunk(markdown);
        assertEquals(1, chunks.size());
        assertEquals(markdown, chunks.get(0).text());
        assertEquals(List.of(), chunks.get(0).headingPath());
        assertEquals(1, chunks.get(0).pageStart());
        assertEquals(1, chunks.get(0).pageEnd());
        assertTrue(chunks.get(0).imageIds().isEmpty());
    }

    @Test
    void sectionsDeTitresDeviennentDesChunksAvecHeadingPath() {
        String markdown = """
                # Chapitre 1

                Contenu du premier chapitre, volontairement court pour tenir dans un chunk.

                ## Section 1.1

                Contenu de la section 1.1.

                ## Section 1.2

                Contenu de la section 1.2.
                """;
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        assertTrue(chunks.size() >= 2);
        // Le premier chunk doit avoir headingPath ["Chapitre 1"]
        assertEquals(List.of("Chapitre 1"), chunks.get(0).headingPath());
        // Le dernier chunk doit avoir headingPath ["Chapitre 1", "Section 1.2"]
        StructuredChunk last = chunks.get(chunks.size() - 1);
        assertEquals(List.of("Chapitre 1", "Section 1.2"), last.headingPath());
    }

    @Test
    void blocCodeEstAtomique() {
        String markdown = "# Titre\n\n```java\npublic class Example {\n" + "x".repeat(2000) + "\n}\n```\n\nAprès le code.";
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        // Le bloc code doit être un chunk séparé, jamais cassé
        boolean hasCodeChunk = chunks.stream()
                .anyMatch(c -> c.elementTypes().contains(ElementType.CODE)
                        && c.text().length() > 1500);
        assertTrue(hasCodeChunk, "Le bloc code doit rester atomique même si > 1500 chars");
    }

    @Test
    void headingPathSuitLaHierarchie() {
        String markdown = """
                # Chapitre A

                Contenu A.

                ## Section A.1

                Contenu A.1.

                # Chapitre B

                Contenu B.
                """;
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        // Au moins 3 chunks : Chapitre A (+ préambule), Section A.1, Chapitre B
        assertTrue(chunks.size() >= 3);
        // Vérifier que le headingPath se réinitialise avec le nouveau Chapitre B
        List<String> lastPath = chunks.get(chunks.size() - 1).headingPath();
        assertEquals(List.of("Chapitre B"), lastPath);
    }

    @Test
    void mergePetitsChunksMemeSection() {
        String markdown = "# Titre\n\nx\n\n" + "a".repeat(200);
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        // Le petit "x" doit être fusionné avec le paragraphe suivant (même section)
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).text().contains("x"));
    }

    @Test
    void chunkIndexEstSequentiel() {
        String markdown = """
                # A

                Contenu A.

                # B

                Contenu B.

                # C

                Contenu C.
                """;
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).chunkIndex());
        }
    }

    @Test
    void pageStartEndToujours1PourNonPDF() {
        String markdown = "# Titre\n\nContenu.";
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        for (StructuredChunk chunk : chunks) {
            assertEquals(1, chunk.pageStart());
            assertEquals(1, chunk.pageEnd());
        }
    }

    @Test
    void documentAvecCodeEtTitres() {
        String markdown = """
                # API

                Voici un exemple :

                ```python
                def hello():
                    print("world")
                ```

                Fin de la section.
                """;
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        assertFalse(chunks.isEmpty());
        // Au moins un chunk avec ElementType.CODE
        assertTrue(chunks.stream().anyMatch(c -> c.elementTypes().contains(ElementType.CODE)));
        // Au moins un chunk avec ElementType.PARAGRAPH
        assertTrue(chunks.stream().anyMatch(c -> c.elementTypes().contains(ElementType.PARAGRAPH)));
    }

    @Test
    void artefactsPdfSontNormalises() {
        String markdown = "# Titre\r\n\r\nPage 1\fPage 2   \n\n\n\nSuite";
        List<StructuredChunk> chunks = chunker.chunk(markdown);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).text().contains("Page 1"));
        assertTrue(chunks.get(0).text().contains("Page 2"));
        assertFalse(chunks.get(0).text().contains("\r"));
        assertFalse(chunks.get(0).text().contains("\f"));
    }
}
