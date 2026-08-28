package mg.esmia.miage.userservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.events.UserEvent;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.exception.ConflictException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.userservice.dto.AuthResponse;
import mg.esmia.miage.userservice.dto.LoginRequest;
import mg.esmia.miage.userservice.dto.RegisterRequest;
import mg.esmia.miage.userservice.dto.UserResponse;
import mg.esmia.miage.userservice.entity.RefreshToken;
import mg.esmia.miage.userservice.entity.User;
import mg.esmia.miage.userservice.repository.RefreshTokenRepository;
import mg.esmia.miage.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        // H1 : le rôle est toujours STUDENT à l'inscription.
        // Le choix Étudiant/Enseignant se fait via l'onboarding (PATCH /me).
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .role(User.Role.STUDENT)
                .build();
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Email ou mot de passe incorrect"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Email ou mot de passe incorrect");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Refresh token invalide"));
        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new BadRequestException("Refresh token expiré, veuillez vous reconnecter");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable"));

        // Rotation : l'ancien refresh token est invalidé à chaque utilisation.
        refreshTokenRepository.delete(stored);
        return buildAuthResponse(user);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable"));
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.delete(user);
        // Déclenche le nettoyage en cascade (spaces, conversations, fiches, etc.)
        // dans TOUS les autres services par événement — jamais par appel synchrone.
        eventPublisher.publish(EventChannels.USER_EVENTS, UserEvent.deleted(userId.toString()));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenService.generateAccessToken(user);
        String rawRefreshToken = generateSecureRandomToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken,
                jwtTokenService.getAccessTokenTtlSeconds(), UserResponse.from(user));
    }

    private String generateSecureRandomToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Les refresh tokens ne sont jamais stockés en clair, seulement leur hash SHA-256. */
    private String hash(String value) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
