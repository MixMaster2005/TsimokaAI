package mg.esmia.miage.analyticsservice.repository;

import mg.esmia.miage.analyticsservice.entity.ActiviteJournaliere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActiviteJournaliereRepository extends JpaRepository<ActiviteJournaliere, UUID> {
    Optional<ActiviteJournaliere> findByUserIdAndSpaceIdAndJour(UUID userId, UUID spaceId, LocalDate jour);
    List<ActiviteJournaliere> findBySpaceIdAndJourBetween(UUID spaceId, LocalDate debut, LocalDate fin);
    void deleteBySpaceId(UUID spaceId);
    void deleteByUserId(UUID userId);
}
