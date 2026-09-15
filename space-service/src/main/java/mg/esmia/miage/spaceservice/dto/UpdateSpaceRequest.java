package mg.esmia.miage.spaceservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSpaceRequest(
        @NotBlank String name,
        String description,
        String subjectTag,
        // Édition manuelle libre du persona (V2) : optionnel, null = inchangé.
        @Size(max = 8000) String assistantPersona) {
}
