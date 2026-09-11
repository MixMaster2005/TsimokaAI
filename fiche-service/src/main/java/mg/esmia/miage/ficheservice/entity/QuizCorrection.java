package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_corrections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizCorrection {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(name = "enseignant_id", nullable = false)
    private UUID enseignantId;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "score_corrige")
    private Integer scoreCorrige;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
