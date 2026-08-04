package mg.esmia.miage.spaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membres_groupe", uniqueConstraints = @UniqueConstraint(columnNames = {"groupe_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreGroupe {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "groupe_id", nullable = false)
    private UUID groupeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_groupe", nullable = false)
    @Builder.Default
    private RoleGroupe roleGroupe = RoleGroupe.MEMBRE;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    public enum RoleGroupe {
        MEMBRE, ANIMATEUR
    }
}
