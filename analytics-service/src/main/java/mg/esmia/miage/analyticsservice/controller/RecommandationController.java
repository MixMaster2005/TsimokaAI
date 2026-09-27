package mg.esmia.miage.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.analyticsservice.dto.StudentDashboardResponse;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoint dédié /api/v1/recommandations (routé séparément par la gateway),
 * réutilise le calcul déjà fait pour le dashboard étudiant.
 */
@RestController
@RequestMapping("/api/v1/recommandations")
@RequiredArgsConstructor
public class RecommandationController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        StudentDashboardResponse dashboard = analyticsService.studentDashboard(UUID.fromString(ctx.userId()), spaceId);
        return ApiResponse.success(dashboard.recommandations(), ctx.requestId());
    }

    /**
     * Accusé de lecture (Lot 1) : positionne {@code lue_le}. Seul le propriétaire
     * de la recommandation peut la marquer (404 sinon, sans distinguer).
     */
    @PatchMapping("/{id}/lue")
    public ApiResponse<?> marquerLue(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        boolean ok = analyticsService.marquerRecommandationLue(UUID.fromString(ctx.userId()), id);
        if (!ok) {
            throw new ResourceNotFoundException("Recommandation introuvable : " + id);
        }
        return ApiResponse.success(Map.of("id", id.toString(), "lue", true), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
