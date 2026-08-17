package mg.esmia.miage.chatservice.rag;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Réordonne des documents candidats (retrieval large) par pertinence à la question,
 * puis n'en garde que le {@code topN}.
 *
 * <p>Équivalent fonctionnel du hook {@code RerankingDocumentPostProcessor} du RAG modulaire
 * de Spring AI 2.0 (non disponible en 1.1.x) : Spring AI 1.1.x n'expose que le hook générique
 * {@code DocumentPostProcessor} — on fournit donc notre propre interface + implémentation.
 */
public interface DocumentReranker {

    /**
     * @param query      question originale de l'utilisateur (non réécrite)
     * @param candidates documents candidats issus du retrieval large (topK élevé)
     * @param topN       nombre de documents à conserver
     * @return les {@code topN} documents les plus pertinents, dans l'ordre de pertinence.
     *         Ne doit jamais lever : en cas d'échec, retourner les {@code topN} premiers.
     */
    List<Document> rerank(String query, List<Document> candidates, int topN);
}