package mg.esmia.miage.analyticsservice.dto;

import mg.esmia.miage.analyticsservice.entity.StatistiqueEspace;

public record NotionStatResponse(String notion, int nbConsultations, int nbQuestions) {
    public static NotionStatResponse from(StatistiqueEspace s) {
        return new NotionStatResponse(s.getNotion(), s.getNbConsultations(), s.getNbQuestions());
    }
}
