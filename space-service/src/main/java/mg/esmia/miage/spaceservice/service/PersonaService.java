package mg.esmia.miage.spaceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ================================ TODO (cœur IA à implémenter) ================================
 * Génération et enrichissement du persona pédagogique (cf. CDC §4.1 et "Base de projet").
 *
 * A implémenter :
 *  1. generateInitialPersona(...)  : appel LLM one-shot (Gemini AI Studio — Gemini 2.5 Flash,
 *     cf. contrat de bascule ACTIVE_LLM_PROVIDER) à partir du nom/tag/description de l'espace,
 *     pour produire les instructions système injectées ensuite dans chat-service.
 *  2. enrichPersonaAfterIngestion(...) : appelé par IngestionEventListener quand un événement
 *     DOCUMENT_READY est reçu. Doit récupérer un échantillon des chunks du document (probablement
 *     via un appel REST à ingestion-service, ou en resouscrivant l'info depuis l'événement lui-même
 *     si on l'enrichit côté producteur) puis fusionner le vocabulaire disciplinaire dans le persona
 *     existant via un nouvel appel LLM.
 *
 * En attendant l'implémentation réelle, un persona "template" déterministe est renvoyé pour que
 * la plateforme reste fonctionnelle de bout en bout (pas de blocage des autres services).
 * ================================================================================================
 */
@Service
@Slf4j
public class PersonaService {

    public String generateInitialPersona(String spaceName, String subjectTag, String description) {
        log.warn("PersonaService.generateInitialPersona: implémentation TODO, retour d'un persona générique");
        String subject = (subjectTag == null || subjectTag.isBlank()) ? spaceName : subjectTag;
        return """
                Tu es un assistant pédagogique spécialisé en %s. Adopte un registre disciplinaire \
                rigoureux, structure tes réponses avec des définitions précises et des exemples \
                concrets. (Persona générique — à remplacer par une génération LLM réelle.)
                """.formatted(subject).strip();
    }

    public String enrichPersonaAfterIngestion(String currentPersona, String documentId, int chunkCount) {
        log.warn("PersonaService.enrichPersonaAfterIngestion: implémentation TODO (document={}, chunks={})",
                documentId, chunkCount);
        // TODO : récupérer un échantillon de contenu du document et fusionner via LLM.
        return currentPersona;
    }
}
