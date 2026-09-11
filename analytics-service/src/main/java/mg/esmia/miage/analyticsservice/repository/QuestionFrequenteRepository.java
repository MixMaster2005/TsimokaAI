package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.QuestionFrequente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionFrequenteRepository extends JpaRepository<QuestionFrequente, UUID> {
    Optional<QuestionFrequente> findBySpaceIdAndQuestionNormalisee(UUID spaceId, String questionNormalisee);
    List<QuestionFrequente> findTop10BySpaceIdOrderByNbOccurrencesDesc(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
}
