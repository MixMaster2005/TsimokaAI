package mg.esmia.miage.analyticsservice.dto;

/**
 * Un point d'évolution de l'activité : nombre d'étudiants actifs
 * (derniereActivite dans la semaine) pour la semaine commençant le lundi
 * {@code semaine} (format ISO yyyy-MM-dd).
 */
public record EvolutionSemaineResponse(String semaine, int nbActifs) {
}
