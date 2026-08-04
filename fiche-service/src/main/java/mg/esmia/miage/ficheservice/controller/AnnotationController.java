package mg.esmia.miage.ficheservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.ficheservice.dto.CreateAnnotationRequest;
import mg.esmia.miage.ficheservice.entity.Annotation;
import mg.esmia.miage.ficheservice.service.AnnotationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiches/{ficheId}/annotations")
@RequiredArgsConstructor
public class AnnotationController {

    private final AnnotationService annotationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Annotation> add(@PathVariable UUID ficheId, @Valid @RequestBody CreateAnnotationRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(annotationService.add(ficheId, UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<Annotation>> list(@PathVariable UUID ficheId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(annotationService.listByFiche(ficheId), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
