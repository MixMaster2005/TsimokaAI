package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizShare {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "groupe_id")
    private UUID groupeId;

    @Column(name = "destinataire_id")
    private UUID destinataireId;

    @Column(name = "partage_par", nullable = false)
    private UUID partagePar;

    @CreationTimestamp
    @Column(name = "shared_at", updatable = false)
    private Instant sharedAt;
}
