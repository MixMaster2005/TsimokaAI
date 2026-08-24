package mg.esmia.miage.chatservice.service;

import mg.esmia.miage.chatservice.dto.Citation;

import java.util.List;
import java.util.UUID;

/**
 * Résultat du pipeline RAG + LLM avant persistance du message ASSISTANT.
 *
 * @param content   contenu de la réponse (ou message d'indisponibilité en fallback)
 * @param chunkIds  IDs des chunks Qdrant réellement utilisés comme contexte (traçabilité)
 * @param modelUsed nom du provider LLM actif (groq | gemini | ollama)
 * @param citations citations lisibles (document source + extrait) dérivées des mêmes
 *                  chunks — vides en fallback (aucun contexte retrievé) ou si le
 *                  document source n'a pas pu être résolu.
 */
public record LlmOutcome(String content, UUID[] chunkIds, String modelUsed, List<Citation> citations) {

    public LlmOutcome(String content, UUID[] chunkIds, String modelUsed) {
        this(content, chunkIds, modelUsed, List.of());
    }
}
