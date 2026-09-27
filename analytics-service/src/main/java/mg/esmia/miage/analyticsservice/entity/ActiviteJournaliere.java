package mg.esmia.miage.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Activité d'un étudiant dans un espace pour un jour UTC (Lot 3).
 *
 * <p>Une ligne par (user, space, jour), alimentée par chaque événement consommé.
 * Sert l'évolution hebdomadaire réelle du dashboard enseignant (étudiants distincts
 * actifs par semaine), là où l'ancienne heuristique sur {@code derniereActivite}
 * sous-comptait l'activité (une seule date par étudiant).
 */
@Entity
@Table(name = "activite_journaliere",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "space_id", "jour"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiviteJournaliere {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    /** Jour UTC de l'activité. */
    @Column(nullable = false)
    private LocalDate jour;

    @Column(name = "nb_questions")
    @Builder.Default
    private Integer nbQuestions = 0;

    @Column(name = "nb_fiches")
    @Builder.Default
    private Integer nbFiches = 0;

    @Column(name = "nb_quiz")
    @Builder.Default
    private Integer nbQuiz = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
