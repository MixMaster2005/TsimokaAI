package mg.esmia.miage.ficheservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.ficheservice.dto.FicheResponse;
import mg.esmia.miage.ficheservice.dto.GenerateFicheRequest;
import mg.esmia.miage.ficheservice.dto.PartageFicheResponse;
import mg.esmia.miage.ficheservice.dto.ShareFicheRequest;
import mg.esmia.miage.ficheservice.service.FicheService;
import mg.esmia.miage.ficheservice.service.ShareService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiches")
@RequiredArgsConstructor
public class FicheController {

    private final FicheService ficheService;
    private final ShareService shareService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FicheResponse> generate(@Valid @RequestBody GenerateFicheRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(ficheService.generate(UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<FicheResponse>> listMine(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(ficheService.listMine(spaceId, UUID.fromString(ctx.userId())), ctx.requestId());
    }

    /**
     * Vue transverse "Mes fiches" : toutes les fiches de l'utilisateur, tous espaces
     * confondus, plus récentes d'abord. Le chemin littéral {@code mine} est déclaré avant
     * le {@code GET /{id}} variable — Spring résout les chemins exacts en priorité.
     */
    @GetMapping("/mine")
    public ApiResponse<List<FicheResponse>> listAllMine() {
        UserContext ctx = authenticated();
        return ApiResponse.success(ficheService.listAllMine(UUID.fromString(ctx.userId())), ctx.requestId());
    }

    /**
     * Toutes les fiches d'un espace — vue enseignant (ADMIN uniquement, 403 sinon).
     * Chemin littéral déclaré avant le {@code GET /{id}} variable.
     */
    @GetMapping("/espace/{spaceId}")
    public ApiResponse<List<FicheResponse>> listForSpace(@PathVariable UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(ficheService.listForSpace(spaceId, ctx.isAdmin()), ctx.requestId());
    }

    @GetMapping("/{id}")
    public ApiResponse<FicheResponse> getById(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(ficheService.getById(id, UUID.fromString(ctx.userId()), ctx.isAdmin()), ctx.requestId());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        ficheService.delete(id, UUID.fromString(ctx.userId()), ctx.isAdmin());
        return ApiResponse.success(null, ctx.requestId());
    }

    @PostMapping("/{id}/share")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PartageFicheResponse> share(@PathVariable UUID id, @Valid @RequestBody ShareFicheRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(shareService.share(id, UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping("/{id}/share")
    public ApiResponse<List<PartageFicheResponse>> listShares(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(shareService.listShares(id), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
