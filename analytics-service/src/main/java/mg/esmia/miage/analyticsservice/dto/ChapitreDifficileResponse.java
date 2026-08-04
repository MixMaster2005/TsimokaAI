package mg.esmia.miage.analyticsservice.dto;

import mg.esmia.miage.analyticsservice.entity.ChapitreDifficile;

public record ChapitreDifficileResponse(String chapitre, double scoreDifficulte) {
    public static ChapitreDifficileResponse from(ChapitreDifficile c) {
        return new ChapitreDifficileResponse(c.getChapitre(), c.getScoreDifficulte());
    }
}
