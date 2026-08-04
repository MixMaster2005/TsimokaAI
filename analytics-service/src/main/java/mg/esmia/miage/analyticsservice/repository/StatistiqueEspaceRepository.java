package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.StatistiqueEspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatistiqueEspaceRepository extends JpaRepository<StatistiqueEspace, UUID> {
    Optional<StatistiqueEspace> findBySpaceIdAndNotion(UUID spaceId, String notion);
    List<StatistiqueEspace> findBySpaceIdOrderByNbQuestionsDesc(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
}
