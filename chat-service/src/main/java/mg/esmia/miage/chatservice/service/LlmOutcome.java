package mg.esmia.miage.chatservice.service;

import mg.esmia.miage.chatservice.dto.Citation;
import mg.esmia.miage.chatservice.dto.StructuredContent;

import java.util.List;
import java.util.UUID;

/**
 * Résultat du pipeline RAG + LLM avant persistance du message ASSISTANT.
 *
 * @param content          contenu brut de la réponse (ou message d'indisponibilité en fallback)
 * @param chunkIds         IDs des chunks Qdrant réellement utilisés comme contexte (traçabilité)
 * @param modelUsed        nom du provider LLM actif (groq | gemini | ollama)
 * @param citations        citations lisibles (document source + extrait) dérivées des mêmes
 *                         chunks — vides en fallback (aucun contexte retrievé) ou si le
 *                         document source n'a pas pu être résolu.
 * @param structuredContent contenu parsé en blocs structurés (code, mermaid, math, images)
 */
public record LlmOutcome(String content, UUID[] chunkIds, String modelUsed,
                         List<Citation> citations, StructuredContent structuredContent) {

    public LlmOutcome(String content, UUID[] chunkIds, String modelUsed, List<Citation> citations) {
        this(content, chunkIds, modelUsed, citations, null);
    }

    public LlmOutcome(String content, UUID[] chunkIds, String modelUsed) {
        this(content, chunkIds, modelUsed, List.of(), null);
    }
}
