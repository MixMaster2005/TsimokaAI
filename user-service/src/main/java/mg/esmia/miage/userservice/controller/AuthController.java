package mg.esmia.miage.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.userservice.dto.AuthResponse;
import mg.esmia.miage.userservice.dto.LoginRequest;
import mg.esmia.miage.userservice.dto.RefreshRequest;
import mg.esmia.miage.userservice.dto.RegisterRequest;
import mg.esmia.miage.userservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Routes PUBLIQUES (non protégées par JWT à la gateway) : /api/v1/auth/**
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), UserContextHolder.get().requestId());
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), UserContextHolder.get().requestId());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()), UserContextHolder.get().requestId());
    }
}
