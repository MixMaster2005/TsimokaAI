package mg.esmia.miage.ficheservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ================================ TODO (cœur IA à implémenter) ================================
 * Génération de fiches par pattern MAP-REDUCE (cf. CDC §4.4) :
 *
 *   MAP    : pour chaque document ciblé, récupérer ses chunks (via un appel REST à
 *            ingestion-service, ou par lecture directe de ses métadonnées si exposées),
 *            puis produire un résumé intermédiaire structuré par un appel LLM one-shot
 *            (Gemini AI Studio — Gemini 2.5 Flash, cf. contrat de bascule provider).
 *   REDUCE : fusionner les résumés intermédiaires en une fiche cohérente unique, respectant
 *            la structure typée attendue : sections "definition", "key_points", "example"
 *            (cf. exemple content_json dans la "Base de projet" Notion).
 *
 * Le résultat doit être sérialisé en JSON et assigné à Fiche.contentJson.
 * ================================================================================================
 */
@Service
@Slf4j
public class FicheGenerationService {

    /**
     * @return le contenu JSON structuré de la fiche (format : voir doc ci-dessus).
     *         Implémentation actuelle : placeholder statique, à remplacer par le pipeline réel.
     */
    public String generateContentJson(java.util.List<java.util.UUID> documentIds) {
        log.warn("FicheGenerationService.generateContentJson : pipeline Map-Reduce non implémenté (TODO), " +
                "contenu placeholder retourné pour {} document(s)", documentIds.size());
        return """
                {
                  "sections": [
                    { "type": "definition", "title": "TODO", "content": "Génération Map-Reduce non implémentée" },
                    { "type": "key_points", "title": "Points clés", "items": [] },
                    { "type": "example", "title": "Exemple appliqué", "content": "" }
                  ]
                }
                """;
    }
}
