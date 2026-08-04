package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.ChapitreDifficile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapitreDifficileRepository extends JpaRepository<ChapitreDifficile, UUID> {
    Optional<ChapitreDifficile> findBySpaceIdAndChapitre(UUID spaceId, String chapitre);
    List<ChapitreDifficile> findBySpaceIdOrderByScoreDifficulteDesc(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
}
