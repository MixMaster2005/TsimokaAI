package mg.esmia.miage.gamificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.gamificationservice.dto.CreateRappelRequest;
import mg.esmia.miage.gamificationservice.dto.RappelResponse;
import mg.esmia.miage.gamificationservice.service.RappelService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rappels")
@RequiredArgsConstructor
public class RappelController {

    private final RappelService rappelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RappelResponse> create(@Valid @RequestBody CreateRappelRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(rappelService.create(UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<RappelResponse>> listMine() {
        UserContext ctx = authenticated();
        return ApiResponse.success(rappelService.listMine(UUID.fromString(ctx.userId())), ctx.requestId());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        UserContext ctx = authenticated();
        rappelService.delete(id, UUID.fromString(ctx.userId()));
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
