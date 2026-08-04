package mg.esmia.miage.spaceservice.repository;

import mg.esmia.miage.spaceservice.entity.MembreGroupe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembreGroupeRepository extends JpaRepository<MembreGroupe, UUID> {
    List<MembreGroupe> findByGroupeId(UUID groupeId);
    Optional<MembreGroupe> findByGroupeIdAndUserId(UUID groupeId, UUID userId);
    void deleteByGroupeId(UUID groupeId);
}
