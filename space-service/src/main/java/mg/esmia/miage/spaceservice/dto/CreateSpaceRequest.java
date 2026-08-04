package mg.esmia.miage.spaceservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSpaceRequest(@NotBlank String name, String description, String subjectTag) {
}
