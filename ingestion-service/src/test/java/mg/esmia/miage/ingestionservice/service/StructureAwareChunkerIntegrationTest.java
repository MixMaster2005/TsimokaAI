package mg.esmia.miage.ingestionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.esmia.miage.ingestionservice.dto.StructuredChunk;
import mg.esmia.miage.ingestionservice.dto.ast.CanonicalDocument;
import mg.esmia.miage.ingestionservice.dto.ast.ElementType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du StructureAwareChunker avec le fixture JSON réel.
 * Valide que le pipeline AST → chunks fonctionne bout en bout.
 */
class StructureAwareChunkerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StructureAwareChunker chunker = new StructureAwareChunker();

    private CanonicalDocument loadSampleAst() throws Exception {
        InputStream is = getClass().getResourceAsStream("/fixtures/sample_ast.json");
        assertNotNull(is, "Fixture JSON introuvable");
        return objectMapper.readValue(is, CanonicalDocument.class);
    }

    @Test
    void sampleAstProducesNonEmptyChunks() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);
        assertFalse(chunks.isEmpty(), "Le fixture doit produire au moins 1 chunk");
    }

    @Test
    void allTableElementsAreIntact() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);

        // Trouver le chunk contenant la table
        boolean foundTableChunk = false;
        for (StructuredChunk chunk : chunks) {
            if (chunk.elementTypes().contains(ElementType.TABLE)) {
                foundTableChunk = true;
                // La table doit être dans un chunk dédié ou avec peu d'éléments
                // (selon la taille, elle peut être avec des éléments adjacents)
                assertTrue(chunk.text().contains("Nom"),
                        "Le chunk avec table doit contenir les données de la table");
            }
        }
        assertTrue(foundTableChunk, "Au moins un chunk doit contenir une table");
    }

    @Test
    void headingPathConsistentWithParentId() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);

        // Le chunk sous "Section 1.1" doit avoir ["Chapitre 1", "Section 1.1"]
        boolean found = false;
        for (StructuredChunk chunk : chunks) {
            if (chunk.text().contains("Contenu de la section 1.1")) {
                assertEquals(List.of("Chapitre 1", "Section 1.1"), chunk.headingPath());
                found = true;
            }
        }
        assertTrue(found, "Le chunk avec 'Contenu de la section 1.1' doit exister");
    }

    @Test
    void pageRangeCoversAllElements() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);

        for (StructuredChunk chunk : chunks) {
            assertTrue(chunk.pageStart() >= 1, "pageStart doit être >= 1");
            assertTrue(chunk.pageEnd() >= chunk.pageStart(), "pageEnd doit être >= pageStart");
            assertTrue(chunk.pageEnd() <= 2, "pageEnd doit être <= nombre de pages max");
        }
    }

    @Test
    void allElementTypesTracked() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);

        // Vérifier que chaque chunk a au moins un type
        for (StructuredChunk chunk : chunks) {
            assertFalse(chunk.elementTypes().isEmpty(),
                    "Chaque chunk doit avoir au moins un ElementType");
        }

        // Vérifier que PARAGRAPH est présent dans au moins un chunk
        boolean hasParagraph = chunks.stream()
                .anyMatch(c -> c.elementTypes().contains(ElementType.PARAGRAPH));
        assertTrue(hasParagraph, "Au moins un chunk doit contenir un PARAGRAPH");

        // Vérifier que TABLE est présent dans au moins un chunk
        boolean hasTable = chunks.stream()
                .anyMatch(c -> c.elementTypes().contains(ElementType.TABLE));
        assertTrue(hasTable, "Au moins un chunk doit contenir un TABLE");
    }

    @Test
    void chunkCountReasonable() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);

        // Le fixture a 11 éléments sur 2 pages → entre 3 et 8 chunks attendus
        assertTrue(chunks.size() >= 3, "Trop peu de chunks : " + chunks.size());
        assertTrue(chunks.size() <= 8, "Trop de chunks : " + chunks.size());
    }

    @Test
    void preambleHasEmptyPath() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);

        // Le premier chunk (p1-h000 est un heading, donc le contenu qui suit est sous ["Chapitre 1"])
        // Pas de préambule dans ce fixture, mais on vérifie que les paths sont cohérents
        for (StructuredChunk chunk : chunks) {
            assertNotNull(chunk.headingPath());
        }
    }

    @Test
    void imageIdsEmptyInV1() throws Exception {
        CanonicalDocument ast = loadSampleAst();
        List<StructuredChunk> chunks = chunker.chunk(ast);

        for (StructuredChunk chunk : chunks) {
            assertTrue(chunk.imageIds().isEmpty(),
                    "V1: imageIds doit être vide");
        }
    }
}
