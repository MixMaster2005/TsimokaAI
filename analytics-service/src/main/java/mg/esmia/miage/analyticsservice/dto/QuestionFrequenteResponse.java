package mg.esmia.miage.analyticsservice.dto;

import mg.esmia.miage.analyticsservice.entity.QuestionFrequente;

import java.time.Instant;

public record QuestionFrequenteResponse(String question, int nbOccurrences, Instant dernierAsk) {
    public static QuestionFrequenteResponse from(QuestionFrequente q) {
        return new QuestionFrequenteResponse(
                q.getQuestionNormalisee(),
                q.getNbOccurrences() == null ? 0 : q.getNbOccurrences(),
                q.getDernierAsk());
    }
}
