package mg.esmia.miage.chatservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.chatservice.dto.ConversationResponse;
import mg.esmia.miage.chatservice.dto.CreateConversationRequest;
import mg.esmia.miage.chatservice.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> create(@Valid @RequestBody CreateConversationRequest request) {
        UserContext ctx = authenticated();
        return ApiResponse.success(conversationService.create(UUID.fromString(ctx.userId()), request), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> listMine(@RequestParam UUID spaceId) {
        UserContext ctx = authenticated();
        return ApiResponse.success(conversationService.listMine(spaceId, UUID.fromString(ctx.userId())), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
