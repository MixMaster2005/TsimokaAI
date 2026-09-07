package mg.esmia.miage.ficheservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers_json", columnDefinition = "jsonb")
    private String answersJson;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private int score = 0;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @CreationTimestamp
    @Column(name = "attempted_at", updatable = false)
    private Instant attemptedAt;
}
