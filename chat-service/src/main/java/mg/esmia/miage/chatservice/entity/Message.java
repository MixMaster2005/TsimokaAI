package mg.esmia.miage.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import mg.esmia.miage.chatservice.dto.Citation;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Chunks utilisés comme contexte pour cette réponse (traçabilité RAG, cf. CDC §4.3).
     * Mappé nativement sur une colonne PostgreSQL UUID[], fidèle au schéma du contrat
     * ("retrieved_chunk_ids UUID[]") plutôt qu'une table de jointure @ElementCollection.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "retrieved_chunk_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] retrievedChunkIds = new UUID[0];

    /**
     * Citations lisibles (document source + extrait) résolues au moment de la génération
     * — cf. {@code dto.Citation}. Vide pour les messages antérieurs à la feature et pour
     * les réponses de fallback du circuit breaker (aucun contexte retrievé).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "citations", columnDefinition = "jsonb")
    @Builder.Default
    private List<Citation> citations = List.of();

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "token_count")
    private Integer tokenCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum Role {
        USER, ASSISTANT
    }
}
