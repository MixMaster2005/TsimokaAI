package mg.esmia.miage.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Lit les headers X-User-Id / X-User-Role / X-Request-Id injectés par l'api-gateway
 * et les expose via UserContextHolder pour toute la durée de la requête.
 * S'applique à chaque service backend (jamais à la gateway elle-même, qui, elle,
 * GÉNÈRE ces headers après vérification du JWT).
 */
@Order(Integer.MIN_VALUE + 10)
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader(UserContext.HEADER_USER_ID);
        String role = request.getHeader(UserContext.HEADER_USER_ROLE);
        String requestId = request.getHeader(UserContext.HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        try {
            UserContextHolder.set(new UserContext(userId, role, requestId));
            response.setHeader(UserContext.HEADER_REQUEST_ID, requestId);
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
