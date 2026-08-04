package mg.esmia.miage.analyticsservice.dto;

import mg.esmia.miage.analyticsservice.entity.Recommandation;

import java.time.Instant;
import java.util.UUID;

public record RecommandationResponse(UUID id, Recommandation.Type type, String contenu, Instant genereLe) {
    public static RecommandationResponse from(Recommandation r) {
        return new RecommandationResponse(r.getId(), r.getType(), r.getContenu(), r.getGenereLe());
    }
}
