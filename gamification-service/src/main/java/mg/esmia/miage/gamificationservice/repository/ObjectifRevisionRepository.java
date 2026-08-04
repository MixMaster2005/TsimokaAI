package mg.esmia.miage.gamificationservice.repository;

import mg.esmia.miage.gamificationservice.entity.ObjectifRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObjectifRevisionRepository extends JpaRepository<ObjectifRevision, UUID> {
    List<ObjectifRevision> findByUserIdAndSpaceId(UUID userId, UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
