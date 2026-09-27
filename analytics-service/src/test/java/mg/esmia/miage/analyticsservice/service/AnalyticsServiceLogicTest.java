package mg.esmia.miage.analyticsservice.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lot 1 : logique pure d'AnalyticsService (sans contexte Spring).
 */
class AnalyticsServiceLogicTest {

    @Test
    void tauxRefleteQuizQuandPasse() {
        // 90 % au meilleur quiz -> 0.9 même avec peu de fiches.
        assertEquals(0.9, AnalyticsService.calculerTauxReussite(10, 1, 2, 90.0, 40.0), 1e-9);
    }

    @Test
    void tauxRepliDernierScoreSiPasDeMeilleur() {
        assertEquals(0.35, AnalyticsService.calculerTauxReussite(5, 5, 1, null, 35.0), 1e-9);
    }

    @Test
    void tauxRepliRatioSansQuiz() {
        assertEquals(0.1, AnalyticsService.calculerTauxReussite(10, 1, 0, null, null), 1e-9);
        assertEquals(0.0, AnalyticsService.calculerTauxReussite(0, 0, 0, null, null), 1e-9);
    }

    @Test
    void tauxBorneZeroUn() {
        assertEquals(1.0, AnalyticsService.calculerTauxReussite(1, 1, 3, 150.0, 150.0), 1e-9);
        assertEquals(0.0, AnalyticsService.calculerTauxReussite(1, 1, 1, -5.0, -5.0), 1e-9);
    }

    @Test
    void extractNotionDeuxMotsMax() {
        String notion = AnalyticsService.extractNotion("Explique la photosynthèse chlorophyllienne en détail ?");
        assertFalse(notion.isBlank());
        assertTrue(notion.length() <= 60);
        // Au plus 2 mots significatifs.
        assertTrue(notion.split(" ").length <= 2);
    }

    @Test
    void extractNotionRepliGeneral() {
        assertEquals("général", AnalyticsService.extractNotion("le la les et ou ?"));
        assertEquals("général", AnalyticsService.extractNotion(null));
        assertEquals("général", AnalyticsService.extractNotion("   "));
    }

    @Test
    void notionKeyEviteCleGenerale() {
        // Question vague : la clé persiste la question tronquée, pas "général".
        assertEquals("général", AnalyticsService.extractNotion("le la les et ou ?"));
        assertNotEquals("général", AnalyticsService.notionKey("le la les et ou ?"));
    }

    @Test
    void contenuHashStableEtHex() {
        String h1 = AnalyticsService.contenuHash("hello");
        String h2 = AnalyticsService.contenuHash("hello");
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
        assertNotEquals(h1, AnalyticsService.contenuHash("world"));
    }
}
