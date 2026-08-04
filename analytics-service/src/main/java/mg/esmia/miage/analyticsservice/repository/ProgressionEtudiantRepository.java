package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.ProgressionEtudiant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressionEtudiantRepository extends JpaRepository<ProgressionEtudiant, UUID> {
    Optional<ProgressionEtudiant> findByUserIdAndSpaceId(UUID userId, UUID spaceId);
    List<ProgressionEtudiant> findBySpaceId(UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
