package mg.esmia.miage.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Question brute posée par les étudiants, normalisée (lowercase/trim/collapse,
 * tronquée à 280 car) et agrégée par espace. Permet le calcul des
 * "questions fréquentes" (top N par nbOccurrences).
 */
@Entity
@Table(name = "question_frequente", uniqueConstraints = @UniqueConstraint(columnNames = {"space_id", "question_normalisee"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionFrequente {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "question_normalisee", length = 280, nullable = false)
    private String questionNormalisee;

    @Column(name = "nb_occurrences")
    @Builder.Default
    private Integer nbOccurrences = 0;

    @Column(name = "dernier_ask")
    private Instant dernierAsk;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
