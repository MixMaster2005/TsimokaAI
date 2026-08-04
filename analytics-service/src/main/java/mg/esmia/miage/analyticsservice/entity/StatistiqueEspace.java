package mg.esmia.miage.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statistique_espace", uniqueConstraints = @UniqueConstraint(columnNames = {"space_id", "notion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatistiqueEspace {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    /** Notion/thème approximatif extrait de la question (cf. AnalyticsService#extractNotion). */
    @Column(nullable = false)
    private String notion;

    @Column(name = "nb_consultations")
    @Builder.Default
    private Integer nbConsultations = 0;

    @Column(name = "nb_questions")
    @Builder.Default
    private Integer nbQuestions = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
