package mg.esmia.miage.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chapitre_difficile", uniqueConstraints = @UniqueConstraint(columnNames = {"space_id", "chapitre"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapitreDifficile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private String chapitre;

    /** Score croissant avec le nombre de questions/rejets liés à ce chapitre. */
    @Column(name = "score_difficulte")
    @Builder.Default
    private Double scoreDifficulte = 0.0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
