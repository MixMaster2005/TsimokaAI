package mg.esmia.miage.ficheservice.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Structure typée du contenu JSON d'une fiche de révision (cf. CDC §4.4 et « Base de projet »).
 * Sérialisée en {@code Fiche.contentJson} et validée/convertie par
 * {@code StructuredOutputValidationAdvisor} + {@code entity(FicheContent.class)}.
 *
 * @param definition     définition générale de la notion
 * @param keyPoints      points clés de la notion (liste)
 * @param example        exemple appliqué
 * @param commonMistakes erreurs fréquentes à éviter (optionnel)
 * @param selfQuiz       questions de révision active pour testing effect (optionnel)
 */
public record FicheContent(
        @JsonProperty("definition") String definition,
        @JsonProperty("key_points") List<String> keyPoints,
        @JsonProperty("example") String example,
        @JsonProperty("common_mistakes") List<String> commonMistakes,
        @JsonProperty("self_quiz") List<QuizQuestion> selfQuiz
) {
    public record QuizQuestion(
            @JsonProperty("question") String question,
            @JsonProperty("answer") String answer
    ) {}

    public static FicheContent empty() {
        return new FicheContent("", List.of(), "", List.of(), List.of());
    }
}