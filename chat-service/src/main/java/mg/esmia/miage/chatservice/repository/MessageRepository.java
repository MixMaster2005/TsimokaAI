package mg.esmia.miage.chatservice.repository;

import mg.esmia.miage.chatservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    void deleteByConversationId(UUID conversationId);

    /** M5 : trouve le dernier message ASSISTANT d'une conversation sans charger tout l'historique. */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :convId AND m.role = 'ASSISTANT' " +
            "AND m.content = :content ORDER BY m.createdAt DESC")
    Optional<Message> findLastAssistantByContent(@Param("convId") UUID conversationId, @Param("content") String content);
}
