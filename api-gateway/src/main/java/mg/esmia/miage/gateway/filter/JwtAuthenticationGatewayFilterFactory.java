package mg.esmia.miage.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.UUID;

/**
 * GatewayFilterFactory appliquée aux routes protégées (toutes sauf /api/v1/auth/**).
 * Extrait le Bearer token, le valide, puis enrichit la requête sortante avec
 * X-User-Id / X-User-Role / X-Request-Id avant de la transmettre au service cible.
 * Contrat : "Le frontend ne communique qu'avec la gateway ; les services backend
 * font confiance aux headers enrichis."
 */
@Component
@Slf4j
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private final JwtService jwtService;

    public JwtAuthenticationGatewayFilterFactory(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    public static class Config {
        // Pas de paramètres pour l'instant ; extensible (ex: rôles requis par route).
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);

            if (authHeaders == null || authHeaders.isEmpty() || !authHeaders.get(0).startsWith("Bearer ")) {
                return unauthorized(exchange, "Token JWT manquant");
            }

            String token = authHeaders.get(0).substring(7);
            try {
                Claims claims = jwtService.parseAndValidate(token);
                String userId = jwtService.extractUserId(claims);
                String role = jwtService.extractRole(claims);
                String requestId = UUID.randomUUID().toString();

                ServerHttpRequest mutated = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .header("X-Request-Id", requestId)
                        .build();

                return chain.filter(exchange.mutate().request(mutated).build());
            } catch (JwtException e) {
                log.warn("JWT invalide : {}", e.getMessage());
                return unauthorized(exchange, "Token JWT invalide ou expiré");
            }
        };
    }

    private reactor.core.publisher.Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        byte[] body = ("""
                {"success":false,"error":{"code":"UNAUTHENTICATED","message":"%s","details":{}},"meta":{}}
                """.formatted(message)).getBytes();
        var buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(buffer));
    }
}
