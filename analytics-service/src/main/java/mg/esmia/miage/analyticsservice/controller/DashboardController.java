package mg.esmia.miage.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.analyticsservice.dto.RecommandationResponse;
import mg.esmia.miage.analyticsservice.dto.StudentDashboardResponse;
import mg.esmia.miage.analyticsservice.dto.StudentRowResponse;
import mg.esmia.miage.analyticsservice.dto.TeacherDashboardResponse;
import mg.esmia.miage.analyticsservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    /**
     * Vue transverse (Lot 3) : dashboards de l'utilisateur authentifié dans tous ses
     * espaces. Remplace les N requêtes parallèles du front ({@code useAllTauxReussite}).
     * Chemin littéral {@code all} déclaré avant tout {@code /{variable}} — Spring
     * résout les chemins exacts en priorité (aucun conflit actuel sous /student).
     */
    @GetMapping("/student/all")
    public ApiResponse<List<StudentDashboardResponse>> studentAll() {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                analyticsService.allStudentDashboards(UUID.fromString(ctx.userId())), ctx.requestId());
    }

    @GetMapping("/teacher")
    public ApiResponse<TeacherDashboardResponse> teacher(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        if (!ctx.isAdmin()) {
            throw new ForbiddenException("Tableau de bord enseignant réservé aux enseignants");
        }
        return ApiResponse.success(analyticsService.teacherDashboard(spaceId), ctx.requestId());
    }

    @GetMapping("/teacher/students")
    public ApiResponse<List<StudentRowResponse>> teacherStudents(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        if (!ctx.isAdmin()) {
            throw new ForbiddenException("Tableau de bord enseignant réservé aux enseignants");
        }
        return ApiResponse.success(analyticsService.teacherStudents(spaceId), ctx.requestId());
    }

    @GetMapping("/teacher/recommandations")
    public ApiResponse<List<RecommandationResponse>> teacherRecommandations(
            @RequestParam UUID spaceId, @RequestParam UUID studentId) {
        UserContext ctx = authenticated();
        if (!ctx.isAdmin()) {
            throw new ForbiddenException("Tableau de bord enseignant réservé aux enseignants");
        }
        boolean appartient = analyticsService.teacherStudents(spaceId).stream()
                .anyMatch(row -> row.userId().equals(studentId));
        if (!appartient) {
            throw new ResourceNotFoundException("Étudiant introuvable dans cet espace");
        }
        return ApiResponse.success(
                analyticsService.studentDashboard(studentId, spaceId).recommandations(), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
