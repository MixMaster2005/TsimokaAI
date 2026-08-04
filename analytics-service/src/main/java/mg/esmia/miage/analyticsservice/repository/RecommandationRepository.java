package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.Recommandation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecommandationRepository extends JpaRepository<Recommandation, UUID> {
    List<Recommandation> findByUserIdAndSpaceIdOrderByGenereLeDesc(UUID userId, UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
