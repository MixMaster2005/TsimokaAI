package mg.esmia.miage.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.userservice.dto.ChangePasswordRequest;
import mg.esmia.miage.userservice.dto.UpdateProfileRequest;
import mg.esmia.miage.userservice.dto.UserResponse;
import mg.esmia.miage.userservice.service.AuthService;
import mg.esmia.miage.userservice.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Routes PROTEGEES (JWT vérifié à la gateway) : /api/v1/users/**
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        UserContext ctx = requireAuthenticated();
        return ApiResponse.success(userService.getById(UUID.fromString(ctx.userId())), ctx.requestId());
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        UserContext ctx = requireAuthenticated();
        return ApiResponse.success(userService.updateProfile(UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserContext ctx = requireAuthenticated();
        userService.changePassword(UUID.fromString(ctx.userId()), request);
        return ApiResponse.success(null, ctx.requestId());
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe() {
        UserContext ctx = requireAuthenticated();
        authService.deleteAccount(UUID.fromString(ctx.userId()));
        return ApiResponse.success(null, ctx.requestId());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        UserContext ctx = requireAuthenticated();
        if (!ctx.isAdmin() && !ctx.owns(id.toString())) {
            throw new ForbiddenException("Accès refusé à ce profil utilisateur");
        }
        return ApiResponse.success(userService.getById(id), ctx.requestId());
    }

    private UserContext requireAuthenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
