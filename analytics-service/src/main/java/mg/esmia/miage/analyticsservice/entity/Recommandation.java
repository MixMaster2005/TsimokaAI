package mg.esmia.miage.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommandations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommandation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @CreationTimestamp
    @Column(name = "generee_le", updatable = false)
    private Instant genereLe;

    public enum Type {
        REVISION_NOTION_FAIBLE, CHAPITRE_DIFFICILE, RELANCE_INACTIVITE
    }
}
