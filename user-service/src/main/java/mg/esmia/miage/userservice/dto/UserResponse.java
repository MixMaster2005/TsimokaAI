package mg.esmia.miage.userservice.dto;

import mg.esmia.miage.userservice.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, User.Role role, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}
