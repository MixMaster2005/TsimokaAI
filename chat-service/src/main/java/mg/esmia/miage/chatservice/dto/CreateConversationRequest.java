package mg.esmia.miage.chatservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateConversationRequest(@NotNull UUID spaceId, String title) {
}
