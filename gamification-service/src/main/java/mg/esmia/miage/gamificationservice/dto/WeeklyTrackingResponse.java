package mg.esmia.miage.gamificationservice.dto;

/**
 * Récapitulatif hebdomadaire de la semaine courante (lundi → dimanche).
 *
 * <p>Construit sans migration (Lot 2) à partir de {@code suivi_hebdomadaire} (compteurs
 * fiches/objectifs de la semaine) + {@code objectif_revision} (total pour le taux,
 * {@code updatedAt} dans la semaine pour les jours actifs — heuristique documentée).
 */
public record WeeklyTrackingResponse(
        /** Lundi de la semaine (ISO, ex. 2026-09-28). */
        String semaine,
        int nbObjectifsAtteints,
        int nbFichesGenerees,
        /** 0.0-1.0 : objectifs atteints cette semaine / total objectifs de l'espace. */
        double tauxProgression,
        /** Jours distincts avec une mise à jour d'objectif ou du suivi dans la semaine. */
        int joursActifs) {
}
