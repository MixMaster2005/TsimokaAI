package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.QuizShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizShareRepository extends JpaRepository<QuizShare, UUID> {

    List<QuizShare> findByQuizId(UUID quizId);

    List<QuizShare> findByDestinataireId(UUID destinataireId);

    void deleteByQuizId(UUID quizId);
}
