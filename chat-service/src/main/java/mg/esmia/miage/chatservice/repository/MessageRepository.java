package mg.esmia.miage.chatservice.repository;

import mg.esmia.miage.chatservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    void deleteByConversationId(UUID conversationId);
}
