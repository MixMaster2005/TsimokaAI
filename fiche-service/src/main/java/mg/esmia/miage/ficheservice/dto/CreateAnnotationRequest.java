package mg.esmia.miage.ficheservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAnnotationRequest(@NotBlank String contenu, String sectionRef) {
}
