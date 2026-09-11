package mg.esmia.miage.ficheservice.controller;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.ficheservice.dto.CreateCorrectionRequest;
import mg.esmia.miage.ficheservice.dto.QuizCorrectionResponse;
import mg.esmia.miage.ficheservice.service.QuizCorrectionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quizzes/{id}")
@RequiredArgsConstructor
public class QuizCorrectionController {

    private final QuizCorrectionService correctionService;

    @GetMapping("/corrections")
    public ApiResponse<List<QuizCorrectionResponse>> listQuizCorrections(@PathVariable("id") UUID quizId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(correctionService.listByQuiz(quizId), ctx.requestId());
    }

    @PostMapping("/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizCorrectionResponse> addQuizCorrection(
            @PathVariable("id") UUID quizId,
            @RequestBody(required = false) CreateCorrectionRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                correctionService.addQuizCorrection(quizId, UUID.fromString(ctx.userId()), ctx.isAdmin(), request),
                ctx.requestId());
    }

    @GetMapping("/attempts/{attemptId}/corrections")
    public ApiResponse<List<QuizCorrectionResponse>> listAttemptCorrections(
            @PathVariable("id") UUID quizId,
            @PathVariable UUID attemptId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(correctionService.listByAttempt(quizId, attemptId), ctx.requestId());
    }

    @PostMapping("/attempts/{attemptId}/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizCorrectionResponse> addAttemptCorrection(
            @PathVariable("id") UUID quizId,
            @PathVariable UUID attemptId,
            @RequestBody(required = false) CreateCorrectionRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                correctionService.addAttemptCorrection(quizId, attemptId, UUID.fromString(ctx.userId()), ctx.isAdmin(), request),
                ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
