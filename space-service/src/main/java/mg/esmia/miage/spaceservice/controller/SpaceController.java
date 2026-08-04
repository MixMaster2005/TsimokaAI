package mg.esmia.miage.spaceservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.spaceservice.dto.CreateSpaceRequest;
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

    @GetMapping
    public ApiResponse<List<SpaceResponse>> listMine() {
        UserContext ctx = authenticated();
        return ApiResponse.success(spaceService.listMine(UUID.fromString(ctx.userId())), ctx.requestId());
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

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
