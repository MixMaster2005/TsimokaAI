package mg.esmia.miage.gamificationservice.repository;

import mg.esmia.miage.gamificationservice.entity.BadgeObtenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BadgeObtenuRepository extends JpaRepository<BadgeObtenu, UUID> {
    List<BadgeObtenu> findByUserId(UUID userId);
    Optional<BadgeObtenu> findByUserIdAndBadgeId(UUID userId, UUID badgeId);
    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);
    void deleteByUserId(UUID userId);
}
