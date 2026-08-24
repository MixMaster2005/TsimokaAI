package mg.esmia.miage.spaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Adhésion d'un utilisateur à un espace de cours qu'il ne possède pas.
 * L'espace reste mono-propriétaire ({@link Space#getUserId()}) : l'adhésion
 * ouvre l'accès en lecture/participation, jamais en écriture sur l'espace lui-même
 * (renommage, suppression, persona...). Pas de rôle ici : le seul rôle qui compte
 * est "propriétaire ou pas".
 */
@Entity
@Table(name = "membres_space", uniqueConstraints = @UniqueConstraint(columnNames = {"space_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreSpace {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    /** Référence logique vers user-service, PAS de foreign key inter-service. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;
}
