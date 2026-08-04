package mg.esmia.miage.gamificationservice.repository;

import mg.esmia.miage.gamificationservice.entity.Rappel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RappelRepository extends JpaRepository<Rappel, UUID> {
    List<Rappel> findByUserId(UUID userId);
    List<Rappel> findByEnvoyeFalseAndPrevuLeBefore(Instant now);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
