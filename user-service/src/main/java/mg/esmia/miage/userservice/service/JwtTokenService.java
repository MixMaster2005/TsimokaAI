package mg.esmia.miage.userservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import mg.esmia.miage.userservice.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Émission des JWT d'accès. La CLÉ DE SIGNATURE DOIT être identique à celle
 * utilisée par api-gateway (variable d'env JWT_SECRET partagée) pour que la
 * gateway puisse vérifier les tokens émis ici.
 */
@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtTokenService(@Value("${security.jwt.secret}") String secret,
                            @Value("${security.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtlSeconds, ChronoUnit.SECONDS)))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}
