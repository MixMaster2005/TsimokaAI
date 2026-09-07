package mg.esmia.miage.ficheservice.dto;

import java.util.UUID;

public record ShareQuizRequest(
        UUID groupeId,
        UUID destinataireId
) {
}
