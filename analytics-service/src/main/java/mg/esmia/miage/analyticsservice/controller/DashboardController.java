package mg.esmia.miage.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.analyticsservice.dto.StudentDashboardResponse;
import mg.esmia.miage.analyticsservice.dto.TeacherDashboardResponse;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalyticsService analyticsService;

    @GetMapping("/student")
    public ApiResponse<StudentDashboardResponse> student(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                analyticsService.studentDashboard(UUID.fromString(ctx.userId()), spaceId), ctx.requestId());
    }

    @GetMapping("/teacher")
    public ApiResponse<TeacherDashboardResponse> teacher(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        if (!ctx.isAdmin()) {
            throw new ForbiddenException("Tableau de bord enseignant réservé aux enseignants");
        }
        return ApiResponse.success(analyticsService.teacherDashboard(spaceId), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
