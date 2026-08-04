package mg.esmia.miage.chatservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.context.UserContextHolder;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.response.ApiResponse;
import mg.esmia.miage.chatservice.dto.MessageResponse;
import mg.esmia.miage.chatservice.dto.SendMessageRequest;
import mg.esmia.miage.chatservice.entity.Conversation;
import mg.esmia.miage.chatservice.service.ChatService;
import mg.esmia.miage.chatservice.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageResponse> send(@PathVariable UUID conversationId, @Valid @RequestBody SendMessageRequest request) {
        UserContext ctx = authenticated();
        Conversation conversation = conversationService.getOwned(conversationId, UUID.fromString(ctx.userId()));
        return ApiResponse.success(chatService.sendMessage(conversation, request), ctx.requestId());
    }

    @GetMapping
    public ApiResponse<List<MessageResponse>> history(@PathVariable UUID conversationId) {
        UserContext ctx = authenticated();
        conversationService.getOwned(conversationId, UUID.fromString(ctx.userId())); // vérifie l'accès
        return ApiResponse.success(chatService.history(conversationId), ctx.requestId());
    }

    private UserContext authenticated() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.userId() == null) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ctx;
    }
}
