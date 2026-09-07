package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitQuizAttemptRequest(
        /** JSON array des réponses : [{"questionIndex": 0, "answer": "..."}] */
        @NotNull String answersJson
) {
}
