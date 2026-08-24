package mg.esmia.miage.spaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space {

    @Id
    @GeneratedValue
    private UUID id;

    /** Référence logique vers user-service, PAS de foreign key inter-service. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "subject_tag")
    private String subjectTag;

    /**
     * Instructions système du LLM pour cet espace. Généré automatiquement à la création
     * (voir PersonaService — TODO IA) puis enrichi après chaque ingestion de document.
     */
    @Column(name = "assistant_persona", columnDefinition = "TEXT")
    private String assistantPersona;

    /**
     * Code d'invitation permettant à un autre étudiant de rejoindre l'espace en
     * lecture/participation (POST /api/v1/spaces/join). Unique, généré à la création,
     * régénérable par le propriétaire (POST /{id}/invite-code/regenerate).
     */
    @Column(name = "invite_code", nullable = false, unique = true, length = 10)
    private String inviteCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
