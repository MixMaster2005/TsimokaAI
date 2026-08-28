package mg.esmia.miage.gamificationservice.repository;

import mg.esmia.miage.gamificationservice.entity.SuiviHebdomadaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuiviHebdomadaireRepository extends JpaRepository<SuiviHebdomadaire, UUID> {
    Optional<SuiviHebdomadaire> findByUserIdAndSpaceIdAndSemaineDebut(UUID userId, UUID spaceId, LocalDate semaineDebut);
    List<SuiviHebdomadaire> findByUserIdAndSpaceId(UUID userId, UUID spaceId);

    /** M7 : somme agrégée des fiches générées pour un user+space, sans charger toutes les lignes. */
    @Query("SELECT COALESCE(SUM(s.nbFichesGenerees), 0) FROM SuiviHebdomadaire s WHERE s.userId = :userId AND s.spaceId = :spaceId")
    long sumNbFichesGenereesByUserIdAndSpaceId(@Param("userId") UUID userId, @Param("spaceId") UUID spaceId);

    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
