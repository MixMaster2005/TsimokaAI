package mg.esmia.miage.analyticsservice.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentDashboardResponse(
        UUID userId, UUID spaceId, double tauxReussite,
        List<String> notionsMaitrisees, List<String> notionsFaibles,
        int nbQuestionsPosees, int nbFichesGenerees, Instant derniereActivite,
        List<RecommandationResponse> recommandations
) {
}
