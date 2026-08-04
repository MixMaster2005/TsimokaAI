package mg.esmia.miage.userservice.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
}
