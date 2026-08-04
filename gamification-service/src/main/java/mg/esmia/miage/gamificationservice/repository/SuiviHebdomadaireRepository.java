package mg.esmia.miage.gamificationservice.repository;

import mg.esmia.miage.gamificationservice.entity.SuiviHebdomadaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuiviHebdomadaireRepository extends JpaRepository<SuiviHebdomadaire, UUID> {
    Optional<SuiviHebdomadaire> findByUserIdAndSpaceIdAndSemaineDebut(UUID userId, UUID spaceId, LocalDate semaineDebut);
    List<SuiviHebdomadaire> findByUserIdAndSpaceId(UUID userId, UUID spaceId);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
