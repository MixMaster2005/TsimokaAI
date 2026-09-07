package mg.esmia.miage.ficheservice.repository;

import mg.esmia.miage.ficheservice.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findBySpaceIdAndUserId(UUID spaceId, UUID userId);

    List<Quiz> findByUserIdOrderByGeneratedAtDesc(UUID userId);

    List<Quiz> findBySpaceId(UUID spaceId);

    void deleteBySpaceId(UUID spaceId);

    void deleteByUserId(UUID userId);
}
