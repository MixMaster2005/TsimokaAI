package mg.esmia.miage.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progression_etudiant", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "space_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressionEtudiant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "taux_reussite")
    @Builder.Default
    private Double tauxReussite = 0.0;

    /** Listes de notions (chaînes libres), stockées en JSON brut. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notions_maitrisees", columnDefinition = "jsonb")
    @Builder.Default
    private String notionsMaitrisees = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notions_faibles", columnDefinition = "jsonb")
    @Builder.Default
    private String notionsFaibles = "[]";

    @Column(name = "nb_questions_posees")
    @Builder.Default
    private Integer nbQuestionsPosees = 0;

    @Column(name = "nb_fiches_generees")
    @Builder.Default
    private Integer nbFichesGenerees = 0;

    @Column(name = "derniere_activite")
    private Instant derniereActivite;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
