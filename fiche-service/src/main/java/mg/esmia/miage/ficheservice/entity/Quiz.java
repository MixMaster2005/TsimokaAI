package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String title;

    @Column(name = "scope", nullable = false)
    @Builder.Default
    private String scope = "DOCUMENT";

    @Column(name = "target_document_id")
    private UUID targetDocumentId;

    @Column(name = "target_topic")
    private String targetTopic;

    @Column(name = "source_fiche_id")
    private UUID sourceFicheId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "source_document_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] sourceDocumentIds = new UUID[0];

    @Column(name = "difficulty", nullable = false)
    @Builder.Default
    private String difficulty = "MOYEN";

    @Column(name = "question_count", nullable = false)
    @Builder.Default
    private int questionCount = 10;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private String contentJson;

    @Column(name = "statut", nullable = false)
    @Builder.Default
    private String statut = "PUBLIE";

    @Column(name = "obsolete")
    @Builder.Default
    private boolean obsolete = false;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Statut {
        BROUILLON, PUBLIE
    }

    public enum Scope {
        DOCUMENT, SPACE, TOPIC
    }

    public enum Difficulty {
        FACILE, MOYEN, DIFFICILE
    }
}
