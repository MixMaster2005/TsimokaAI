package mg.esmia.miage.spaceservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSpaceRequest(@NotBlank String name, String description, String subjectTag) {
}
