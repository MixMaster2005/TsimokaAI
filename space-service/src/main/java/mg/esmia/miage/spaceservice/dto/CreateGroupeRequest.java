package mg.esmia.miage.spaceservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupeRequest(@NotBlank String nom, String description) {
}
