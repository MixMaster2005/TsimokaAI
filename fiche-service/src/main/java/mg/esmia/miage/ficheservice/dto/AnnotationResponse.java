package mg.esmia.miage.ficheservice.dto;

import mg.esmia.miage.ficheservice.entity.Annotation;

import java.time.Instant;
import java.util.UUID;

public record AnnotationResponse(UUID id, UUID ficheId, UUID auteurId, String contenu, String sectionRef, Instant createdAt) {
    public static AnnotationResponse from(Annotation a) {
        return new AnnotationResponse(a.getId(), a.getFicheId(), a.getAuteurId(), a.getContenu(), a.getSectionRef(), a.getCreatedAt());
    }
}
