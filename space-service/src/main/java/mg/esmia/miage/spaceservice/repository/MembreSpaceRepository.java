package mg.esmia.miage.spaceservice.repository;

import mg.esmia.miage.spaceservice.entity.MembreSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembreSpaceRepository extends JpaRepository<MembreSpace, UUID> {
    List<MembreSpace> findBySpaceIdOrderByJoinedAtAsc(UUID spaceId);
    List<MembreSpace> findByUserId(UUID userId);
    boolean existsBySpaceIdAndUserId(UUID spaceId, UUID userId);
    void deleteBySpaceIdAndUserId(UUID spaceId, UUID userId);
}
