package mg.esmia.miage.chatservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.chatservice.dto.ConversationResponse;
import mg.esmia.miage.chatservice.dto.CreateConversationRequest;
import mg.esmia.miage.chatservice.entity.Conversation;
import mg.esmia.miage.chatservice.repository.ConversationRepository;
import mg.esmia.miage.chatservice.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ConversationResponse create(UUID userId, CreateConversationRequest request) {
        Conversation conversation = Conversation.builder()
                .spaceId(request.spaceId())
                .userId(userId)
                .title(request.title() == null || request.title().isBlank() ? "Nouvelle conversation" : request.title())
                .build();
        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    public List<ConversationResponse> listMine(UUID spaceId, UUID userId) {
        return conversationRepository.findBySpaceIdAndUserId(spaceId, userId).stream()
                .map(ConversationResponse::from).toList();
    }

    public Conversation getOwned(UUID id, UUID requesterId) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable : " + id));
        if (!conversation.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à cette conversation");
        }
        return conversation;
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        conversationRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        conversationRepository.deleteByUserId(userId);
    }
}
