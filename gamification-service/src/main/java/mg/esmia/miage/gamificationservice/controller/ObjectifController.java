package mg.esmia.miage.gamificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.gamificationservice.dto.CreateObjectifRequest;
import mg.esmia.miage.gamificationservice.dto.ObjectifResponse;
import mg.esmia.miage.gamificationservice.dto.UpdateObjectifRequest;
import mg.esmia.miage.gamificationservice.service.ObjectifService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/objectifs")
@RequiredArgsConstructor
public class ObjectifController {

    private final ObjectifService objectifService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ObjectifResponse> create(@Valid @RequestBody CreateObjectifRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(objectifService.create(UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<ObjectifResponse>> listMine(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(objectifService.listMine(UUID.fromString(ctx.userId()), spaceId), ctx.requestId());
    }

    @PatchMapping("/{id}")
    public ApiResponse<ObjectifResponse> updateStatut(@PathVariable UUID id, @Valid @RequestBody UpdateObjectifRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(objectifService.updateStatut(id, UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
