package mg.esmia.miage.analyticsservice.dto;

import java.util.List;
import java.util.UUID;

public record TeacherDashboardResponse(
        UUID spaceId,
        List<NotionStatResponse> notionsLesPlusConsultees,
        List<ChapitreDifficileResponse> chapitresDifficiles,
        int nbEtudiantsActifs
) {
}
