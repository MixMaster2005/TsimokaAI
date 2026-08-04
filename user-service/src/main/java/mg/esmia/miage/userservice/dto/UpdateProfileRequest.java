package mg.esmia.miage.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(@NotBlank String displayName) {
}
