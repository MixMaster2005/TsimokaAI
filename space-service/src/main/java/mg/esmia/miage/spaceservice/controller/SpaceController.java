package mg.esmia.miage.spaceservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.spaceservice.dto.CreateSpaceRequest;
import mg.esmia.miage.spaceservice.dto.InviteCodeResponse;
import mg.esmia.miage.spaceservice.dto.JoinSpaceRequest;
import mg.esmia.miage.spaceservice.dto.MembreSpaceResponse;
import mg.esmia.miage.spaceservice.dto.SpaceResponse;
import mg.esmia.miage.spaceservice.dto.UpdateSpaceRequest;
import mg.esmia.miage.spaceservice.service.SpaceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SpaceResponse> create(@Valid @RequestBody CreateSpaceRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(spaceService.create(UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    /**
     * Rejoint un espace via son code d'invitation. Chemin littéral déclaré avant le
     * {@code GET /{id}} / {@code PUT /{id}} variables — Spring résout les chemins
     * exacts en priorité.
     */
    @PostMapping("/join")
    public ApiResponse<SpaceResponse> join(@Valid @RequestBody JoinSpaceRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(spaceService.joinByCode(UUID.fromString(ctx.userId()), request.code()),
                ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<SpaceResponse>> listMine() {
        UserContext ctx = authenticated();
        return ApiResponse.success(spaceService.listMine(UUID.fromString(ctx.userId())), ctx.requestId());
    }

    /** Vue de supervision enseignant : tous les espaces (ADMIN uniquement, 403 sinon). */
    @GetMapping("/all")
    public ApiResponse<List<SpaceResponse>> listAll() {
        UserContext ctx = authenticated();
        return ApiResponse.success(spaceService.listAll(ctx.isAdmin()), ctx.requestId());
    }

    @GetMapping("/{id}")
    public ApiResponse<SpaceResponse> getById(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                spaceService.getById(id, UUID.fromString(ctx.userId()), ctx.isAdmin()), ctx.requestId());
    }

    @PutMapping("/{id}")
    public ApiResponse<SpaceResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateSpaceRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                spaceService.update(id, UUID.fromString(ctx.userId()), ctx.isAdmin(), request), ctx.requestId());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        spaceService.delete(id, UUID.fromString(ctx.userId()), ctx.isAdmin());
        return ApiResponse.success(null, ctx.requestId());
    }

    // --- Membres (adhésions via code d'invitation) --------------------------

    @GetMapping("/{id}/membres")
    public ApiResponse<List<MembreSpaceResponse>> listMembres(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(
                spaceService.listMembres(id, UUID.fromString(ctx.userId()), ctx.isAdmin()).stream()
                        .map(MembreSpaceResponse::from).toList(),
                ctx.requestId());
    }

    /** Le propriétaire retire un membre. */
    @DeleteMapping("/{id}/membres/{memberId}")
    public ApiResponse<Void> removeMembre(@PathVariable UUID id, @PathVariable UUID memberId) {
        UserContext ctx = authenticated();
        spaceService.removeMembre(id, memberId, UUID.fromString(ctx.userId()), ctx.isAdmin());
        return ApiResponse.success(null, ctx.requestId());
    }

    /** Un membre quitte l'espace de lui-même ("me" littéral, prioritaire sur {memberId}). */
    @DeleteMapping("/{id}/membres/me")
    public ApiResponse<Void> leave(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        spaceService.leave(id, UUID.fromString(ctx.userId()));
        return ApiResponse.success(null, ctx.requestId());
    }

    // --- Code d'invitation (propriétaire uniquement) -------------------------

    @GetMapping("/{id}/invite-code")
    public ApiResponse<InviteCodeResponse> getInviteCode(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(spaceService.getInviteCode(id, UUID.fromString(ctx.userId()), ctx.isAdmin()),
                ctx.requestId());
    }

    @PostMapping("/{id}/invite-code/regenerate")
    public ApiResponse<InviteCodeResponse> regenerateInviteCode(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        return ApiResponse.success(spaceService.regenerateInviteCode(id, UUID.fromString(ctx.userId()), ctx.isAdmin()),
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
