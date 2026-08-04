package mg.esmia.miage.spaceservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.spaceservice.dto.AddMembreRequest;
import mg.esmia.miage.spaceservice.dto.CreateGroupeRequest;
import mg.esmia.miage.spaceservice.dto.GroupeResponse;
import mg.esmia.miage.spaceservice.dto.MembreGroupeResponse;
import mg.esmia.miage.spaceservice.service.GroupeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GroupeController {

    private final GroupeService groupeService;

    @PostMapping("/api/v1/spaces/{spaceId}/groupes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupeResponse> create(@PathVariable UUID spaceId, @Valid @RequestBody CreateGroupeRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(groupeService.create(spaceId, UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping("/api/v1/spaces/{spaceId}/groupes")
    public ApiResponse<List<GroupeResponse>> listBySpace(@PathVariable UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(groupeService.listBySpace(spaceId), ctx.requestId());
    }

    @PostMapping("/api/v1/groupes/{groupeId}/membres")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MembreGroupeResponse> addMembre(@PathVariable UUID groupeId, @Valid @RequestBody AddMembreRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(groupeService.addMembre(groupeId, request), ctx.requestId());
    }

    @GetMapping("/api/v1/groupes/{groupeId}/membres")
    public ApiResponse<List<MembreGroupeResponse>> listMembres(@PathVariable UUID groupeId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(groupeService.listMembres(groupeId), ctx.requestId());
    }

    @DeleteMapping("/api/v1/groupes/{groupeId}")
    public ApiResponse<Void> delete(@PathVariable UUID groupeId) {
        UserContext ctx = authenticated();
        groupeService.delete(groupeId);
        return ApiResponse.success(null, ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
