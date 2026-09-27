package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.StatistiqueNotionUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatistiqueNotionUserRepository extends JpaRepository<StatistiqueNotionUser, UUID> {
    Optional<StatistiqueNotionUser> findByUserIdAndSpaceIdAndNotion(UUID userId, UUID spaceId, String notion);
    List<StatistiqueNotionUser> findByUserIdAndSpaceIdOrderByNbQuestionsDesc(UUID userId, UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
