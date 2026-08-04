package mg.esmia.miage.ficheservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.ficheservice.dto.ValidateFicheRequest;
import mg.esmia.miage.ficheservice.entity.ValidationFiche;
import mg.esmia.miage.ficheservice.service.ValidationService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiches/{ficheId}/validation")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    @PutMapping
    public ApiResponse<ValidationFiche> validate(@PathVariable UUID ficheId, @Valid @RequestBody ValidateFicheRequest request) {
        UserContext ctx = authenticatedTeacher();
        return ApiResponse.success(validationService.validate(ficheId, UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<ValidationFiche> get(@PathVariable UUID ficheId) {
        UserContext ctx = UserContextHolder.get();
        return ApiResponse.success(validationService.getByFiche(ficheId), ctx.requestId());
    }

    private UserContext authenticatedTeacher() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        if (!ctx.isAdmin()) {
            throw new ForbiddenException("Seul un enseignant peut valider une fiche");
        }
        return ctx;
    }
}
