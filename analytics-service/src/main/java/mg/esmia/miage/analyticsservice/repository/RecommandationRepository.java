package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.Recommandation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommandationRepository extends JpaRepository<Recommandation, UUID> {
    List<Recommandation> findByUserIdAndSpaceIdOrderByGenereLeDesc(UUID userId, UUID spaceId);
    Optional<Recommandation> findFirstByUserIdAndSpaceIdAndTypeAndContenuHashOrderByGenereLeDesc(
            UUID userId, UUID spaceId, Recommandation.Type type, String contenuHash);
    List<Recommandation> findByUserIdAndSpaceIdAndTypeAndGenereLeAfter(
            UUID userId, UUID spaceId, Recommandation.Type type, Instant after);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
    void deleteByGenereLeBefore(Instant seuil);
}
