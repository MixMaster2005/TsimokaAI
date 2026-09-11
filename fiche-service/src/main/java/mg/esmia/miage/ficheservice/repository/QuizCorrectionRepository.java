package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.QuizCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizCorrectionRepository extends JpaRepository<QuizCorrection, UUID> {

    List<QuizCorrection> findByQuizId(UUID quizId);

    List<QuizCorrection> findByAttemptId(UUID attemptId);
}
