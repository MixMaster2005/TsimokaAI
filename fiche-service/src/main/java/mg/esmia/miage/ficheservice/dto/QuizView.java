package mg.esmia.miage.ficheservice.dto;

/**
 * Marqueur commun pour les vues quiz (publique vs détail enseignant).
 */
public sealed interface QuizView permits QuizPublicResponse, QuizDetailResponse {
}
