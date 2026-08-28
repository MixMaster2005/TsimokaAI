package mg.esmia.miage.ingestionservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires du découpage orienté sens (MarkdownChunkingService).
 *
 * <p>Les seuils étant privés, on valide le comportement observable : sections = chunks,
 * titres jamais séparés de leur contenu, re-découpage récursif sur les sous-titres, et
 * découpe de secours (sans couper un mot) uniquement pour une section sans sous-titres.
 */
class MarkdownChunkingServiceTest {

    private final MarkdownChunkingService service = new MarkdownChunkingService();

    private static final int MAX_CHUNK_CHARS = 500 * 3; // aligné sur CHUNK_SIZE_TOKENS * CHARS_PER_TOKEN (3 pour le français)

    private static String repeat(String s, int n) {
        return s.repeat(n);
    }

    @Test
    void videOuNullDonneAucunChunk() {
        assertTrue(service.chunk(null).isEmpty());
        assertTrue(service.chunk("").isEmpty());
        assertTrue(service.chunk("   \n  \n").isEmpty());
    }

    @Test
    void petitDocumentEstUnSeulChunkAvecSonTitre() {
        String markdown = "# Titre\n\nUn paragraphe de contenu, court, sans structure de titres.";
        List<String> chunks = service.chunk(markdown);
        assertEquals(1, chunks.size());
        assertEquals(markdown, chunks.get(0));
    }

    @Test
    void artefactsPdfSontNormalisesAvantChunking() {
        String markdown = "# Titre\r\n\r\nPage 1\fPage 2   \n\n\n\nSuite";
        List<String> chunks = service.chunk(markdown);

        assertEquals(1, chunks.size());
        assertEquals("# Titre\n\nPage 1\n\nPage 2\n\nSuite", chunks.get(0));
    }

    @Test
    void sectionsDeTitresDeviennentDesChunks() {
        String markdown = """
                # Chapitre 1

                Contenu du premier chapitre, volontairement court pour tenir dans un chunk.

                ## Section 1.1

                Contenu de la section 1.1.

                ## Section 1.2

                Contenu de la section 1.2.
                """;
        List<String> chunks = service.chunk(markdown);
        // 2 chunks : # Chapitre 1 (+ préambule fusionné avec ## Section 1.1 car < MIN_CHUNK_CHARS),
        // ## Section 1.2.
        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).startsWith("# Chapitre 1"));
        assertTrue(chunks.get(chunks.size() - 1).startsWith("## Section 1.2"));
    }

    @Test
    void sectionTropGrandeRedecoupeeSurLesSousTitres() {
        // Corps parent > 1500 chars (nouveau MAX_CHUNK_CHARS) : re-découpage sur ##.
        String body = repeat("Contenu parent, volontairement proche de la limite. ", 40);
        String markdown = "# Grande Section\n\n" + body + "\n\n"
                + "## Sous A\n\n" + repeat("a", 220) + "\n\n"
                + "## Sous B\n\n" + repeat("b", 220) + "\n";
        List<String> chunks = service.chunk(markdown);

        assertTrue(markdown.length() > MAX_CHUNK_CHARS);
        assertTrue(chunks.size() >= 3, "Au moins 3 chunks attendus, got " + chunks.size());
        assertTrue(chunks.get(0).startsWith("# Grande Section"));
        // Au moins un chunk commence par ## Sous A, et le dernier par ## Sous B
        assertTrue(chunks.stream().anyMatch(c -> c.startsWith("## Sous A")));
        assertTrue(chunks.get(chunks.size() - 1).startsWith("## Sous B"));
    }

    @Test
    void sousSectionEncoreTropGrandeRedecoupeeRecursivement() {
        // Une ## qui dépasse à son tour -> re-découpage sur ###.
        String longSub = repeat("Paragraphe de la sous-section, encore plus long. ", 80);
        String markdown = "# Titre\n\n## Moyenne\n\n" + longSub + "\n\n### Detail 1\n\nPetit.\n\n### Detail 2\n\nPetit.\n";
        List<String> chunks = service.chunk(markdown);

        assertTrue(longSub.length() > MAX_CHUNK_CHARS);
        // Le premier chunk (## Moyenne sans ###) est trop grand : il doit être re-découpé.
        // Résultat : # Titre (+ préambule), ## Moyenne (première partie), ### Detail 1, ### Detail 2.
        assertTrue(chunks.size() >= 3);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= MAX_CHUNK_CHARS);
            assertFalse(chunk.isBlank());
        }
    }

    @Test
    void sectionSansSousTitreTropGrandeUtiliseLaDecoupeFixeSansCouperUnMot() {
        // Pas de titre du tout : découpe de secours, sur un séparateur d'espace.
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < MAX_CHUNK_CHARS + 500; i++) {
            text.append('a');
        }
        // On garantit un espace exactement à la position MAX_CHUNK_CHARS pour vérifier
        // que la frontière tombe sur un espace et pas au milieu d'un mot.
        text.setCharAt(MAX_CHUNK_CHARS, ' ');
        String markdown = text.toString();

        List<String> chunks = service.chunk(markdown);
        assertTrue(chunks.size() >= 2);
        assertEquals(text.substring(0, MAX_CHUNK_CHARS), chunks.get(0));
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= MAX_CHUNK_CHARS);
            assertFalse(chunk.isBlank());
        }
    }

    @Test
    void decoupeDeSecoursPrefereUneFrontiereDeParagraphe() {
        // Premier paragraphe assez grand pour déclencher la découpe de secours
        String premierParagraphe = repeat("Phrase de contexte pour le premier paragraphe. ", 25)
                + "Fin du premier paragraphe.";
        String secondParagraphe = repeat("Suite du contenu pedagogique. ", 35);
        String markdown = premierParagraphe + "\n\n" + secondParagraphe;

        List<String> chunks = service.chunk(markdown);

        assertTrue(markdown.length() > MAX_CHUNK_CHARS);
        assertTrue(chunks.size() >= 2);
        // Le premier chunk doit se terminer sur une frontière de phrase ou de paragraphe
        String firstChunk = chunks.get(0);
        assertTrue(firstChunk.endsWith(".") || firstChunk.contains("\n\n"),
                "Le premier chunk devrait se terminer sur une frontière de phrase ou paragraphe, mais se termine par: " + firstChunk.substring(Math.max(0, firstChunk.length() - 30)));
    }

    @Test
    void preambuleSansTitreResteAttacheAuContenu() {
        String preamble = "Préambule libre, avant tout titre. " + repeat("x", 1600);
        String markdown = preamble + "\n\n## Sous\n\nContenu.\n";
        List<String> chunks = service.chunk(markdown);

        // Le préambule est trop grand (> 1500) et sans sous-titre : découpe de secours.
        assertTrue(preamble.length() > MAX_CHUNK_CHARS);
        assertTrue(chunks.get(0).startsWith("Préambule libre"));
        assertTrue(chunks.get(0).length() <= MAX_CHUNK_CHARS);
    }

    @Test
    void estimateTokenCountSuitLHeuristiqueCharsSurTrois() {
        assertEquals(1, service.estimateTokenCount("abc"));        // 3 / 3 -> 1
        assertEquals(2, service.estimateTokenCount("abcdefgh"));   // 8 / 3 -> 2
        assertEquals(0, service.estimateTokenCount(null));
    }
}
