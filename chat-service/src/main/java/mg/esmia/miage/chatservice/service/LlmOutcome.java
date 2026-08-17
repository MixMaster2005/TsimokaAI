package mg.esmia.miage.chatservice.service;

import java.util.UUID;

/**
 * Résultat du pipeline RAG + LLM avant persistance du message ASSISTANT.
 *
 * @param content   contenu de la réponse (ou message d'indisponibilité en fallback)
 * @param chunkIds  IDs des chunks Qdrant réellement utilisés comme contexte (traçabilité)
 * @param modelUsed nom du provider LLM actif (groq | gemini | ollama)
 */
public record LlmOutcome(String content, UUID[] chunkIds, String modelUsed) {
}