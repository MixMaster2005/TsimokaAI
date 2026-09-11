package mg.esmia.miage.analyticsservice.dto;

import mg.esmia.miage.analyticsservice.entity.ProgressionEtudiant;

import java.time.Instant;
import java.util.UUID;

public record StudentRowResponse(
        UUID userId, Instant derniereActivite,
        int nbQuestions, int nbFiches, int nbQuizPasses, double taux) {
    public static StudentRowResponse from(ProgressionEtudiant p) {
        return new StudentRowResponse(
                p.getUserId(),
                p.getDerniereActivite(),
                p.getNbQuestionsPosees() == null ? 0 : p.getNbQuestionsPosees(),
                p.getNbFichesGenerees() == null ? 0 : p.getNbFichesGenerees(),
                p.getNbQuizPasses() == null ? 0 : p.getNbQuizPasses(),
                p.getTauxReussite() == null ? 0.0 : p.getTauxReussite());
    }
}
