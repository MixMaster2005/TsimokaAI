package mg.esmia.miage.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Compteur de questions par (étudiant, espace, notion).
 *
 * <p>Complète {@link StatistiqueEspace} (agrégat global par espace pour le dashboard
 * enseignant) : cette table alimente {@code notionsFaibles / notionsMaitrisees} du
 * dashboard étudiant, qui doivent être personnelles et non globales au space (Lot 1).
 */
@Entity
@Table(name = "statistique_notion_user",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "space_id", "notion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatistiqueNotionUser {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    /** Notion extraite de la question (cf. AnalyticsService#extractNotion). */
    @Column(nullable = false)
    private String notion;

    @Column(name = "nb_questions")
    @Builder.Default
    private Integer nbQuestions = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
