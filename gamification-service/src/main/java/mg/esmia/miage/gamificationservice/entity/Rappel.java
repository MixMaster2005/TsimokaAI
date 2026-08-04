package mg.esmia.miage.gamificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rappels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rappel {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(nullable = false)
    private String message;

    @Column(name = "prevu_le", nullable = false)
    private Instant prevuLe;

    @Column(nullable = false)
    @Builder.Default
    private boolean envoye = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
