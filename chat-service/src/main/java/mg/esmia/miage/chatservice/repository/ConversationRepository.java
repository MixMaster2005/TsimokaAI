package mg.esmia.miage.chatservice.repository;

import mg.esmia.miage.chatservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findBySpaceIdAndUserId(UUID spaceId, UUID userId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
