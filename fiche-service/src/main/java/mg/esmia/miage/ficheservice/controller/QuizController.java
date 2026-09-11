package mg.esmia.miage.ficheservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.ficheservice.dto.*;
import mg.esmia.miage.ficheservice.service.QuizAttemptService;
import mg.esmia.miage.ficheservice.service.QuizService;
import mg.esmia.miage.ficheservice.service.QuizShareService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuizAttemptService attemptService;
    private final QuizShareService shareService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizView> generate(@Valid @RequestBody GenerateQuizRequest request) {
        UserContext ctx = authenticated();
        // Le créateur (owner) a un accès complet (détail avec réponses).
        return ApiResponse.success(quizService.generate(UUID.fromString(ctx.userId()), request, true), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<QuizView>> listMine(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        // Ses propres quiz : accès complet.
        return ApiResponse.success(quizService.listMine(spaceId, UUID.fromString(ctx.userId()), true), ctx.requestId());
    }

    @GetMapping("/mine")
    public ApiResponse<List<QuizView>> listAllMine() {
        UserContext ctx = authenticated();
        return ApiResponse.success(quizService.listAllMine(UUID.fromString(ctx.userId()), true), ctx.requestId());
    }

    @GetMapping("/espace/{spaceId}")
    public ApiResponse<List<QuizView>> listForSpace(@PathVariable UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(quizService.listForSpace(spaceId, ctx.isAdmin(), ctx.isAdmin()), ctx.requestId());
    }

    @GetMapping("/{id}")
    public ApiResponse<QuizView> getById(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        // fullAccess=true si admin ; le service l'élargit aussi au owner.
        return ApiResponse.success(quizService.getById(id, UUID.fromString(ctx.userId()), ctx.isAdmin(), ctx.isAdmin()), ctx.requestId());
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<QuizView> publish(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(quizService.publish(id, UUID.fromString(ctx.userId()), ctx.isAdmin()), ctx.requestId());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        quizService.delete(id, UUID.fromString(ctx.userId()), ctx.isAdmin());
        return ApiResponse.success(null, ctx.requestId());
    }

    // --- Partages ---

    @PostMapping("/{id}/share")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizShareResponse> share(@PathVariable UUID id, @Valid @RequestBody ShareQuizRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(shareService.share(id, UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping("/{id}/share")
    public ApiResponse<List<QuizShareResponse>> listShares(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(shareService.listShares(id), ctx.requestId());
    }

    // --- Tentatives ---

    @PostMapping("/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizAttemptResponse> submitAttempt(@PathVariable UUID id,
                                                          @Valid @RequestBody SubmitQuizAttemptRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(attemptService.submit(id, UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping("/{id}/attempts/mine")
    public ApiResponse<List<QuizAttemptResponse>> listMyAttempts(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(attemptService.listMyAttempts(id, UUID.fromString(ctx.userId())), ctx.requestId());
    }

    @GetMapping("/{id}/attempts/stats")
    public ApiResponse<List<QuizAttemptResponse>> listAllAttempts(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                attemptService.listAllAttempts(id, UUID.fromString(ctx.userId()), ctx.isAdmin()),
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
