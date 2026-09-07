package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByQuizIdAndUserIdOrderByAttemptedAtDesc(UUID quizId, UUID userId);

    List<QuizAttempt> findByQuizIdOrderByAttemptedAtDesc(UUID quizId);

    void deleteByQuizId(UUID quizId);
}
