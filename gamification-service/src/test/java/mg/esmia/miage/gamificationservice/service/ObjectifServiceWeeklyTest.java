package mg.esmia.miage.gamificationservice.service;

import mg.esmia.miage.gamificationservice.dto.WeeklyTrackingResponse;
import mg.esmia.miage.gamificationservice.entity.ObjectifRevision;
import mg.esmia.miage.gamificationservice.entity.SuiviHebdomadaire;
import mg.esmia.miage.gamificationservice.repository.ObjectifRevisionRepository;
import mg.esmia.miage.gamificationservice.repository.SuiviHebdomadaireRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Lot 2 : récapitulatif hebdomadaire depuis suivi_hebdomadaire (zéros si vide).
 */
@ExtendWith(MockitoExtension.class)
class ObjectifServiceWeeklyTest {

    @Mock
    ObjectifRevisionRepository objectifRepository;

    @Mock
    GamificationService gamificationService;

    @Mock
    SuiviHebdomadaireRepository suiviRepository;

    @InjectMocks
    ObjectifService objectifService;

    @Test
    void sansActivite_retourneZeros() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        when(suiviRepository.findByUserIdAndSpaceIdAndSemaineDebut(eq(userId), eq(spaceId), any()))
                .thenReturn(Optional.empty());
        when(objectifRepository.findByUserIdAndSpaceId(userId, spaceId)).thenReturn(List.of());

        WeeklyTrackingResponse res = objectifService.weeklyTracking(userId, spaceId);

        LocalDate lundi = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertEquals(lundi.toString(), res.semaine());
        assertEquals(0, res.nbObjectifsAtteints());
        assertEquals(0, res.nbFichesGenerees());
        assertEquals(0.0, res.tauxProgression());
        assertEquals(0, res.joursActifs());
    }

    @Test
    void avecSuiviEtObjectifs_calculeTauxEtJours() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        LocalDate lundi = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        SuiviHebdomadaire suivi = SuiviHebdomadaire.builder()
                .userId(userId).spaceId(spaceId).semaineDebut(lundi)
                .nbFichesGenerees(3).nbObjectifsAtteints(2)
                .updatedAt(lundi.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3600))
                .build();
        when(suiviRepository.findByUserIdAndSpaceIdAndSemaineDebut(userId, spaceId, lundi))
                .thenReturn(Optional.of(suivi));

        ObjectifRevision o1 = ObjectifRevision.builder()
                .userId(userId).spaceId(spaceId).titre("a").statut(ObjectifRevision.Statut.ATTEINT)
                .updatedAt(lundi.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(100))
                .build();
        ObjectifRevision o2 = ObjectifRevision.builder()
                .userId(userId).spaceId(spaceId).titre("b").statut(ObjectifRevision.Statut.ATTEINT)
                .updatedAt(lundi.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(100))
                .build();
        ObjectifRevision vieux = ObjectifRevision.builder()
                .userId(userId).spaceId(spaceId).titre("c").statut(ObjectifRevision.Statut.EN_COURS)
                .updatedAt(lundi.minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant())
                .build();
        // 4e objectif sans updatedAt (jamais persisté) : compte dans le total, pas dans les jours.
        ObjectifRevision sansDate = ObjectifRevision.builder()
                .userId(userId).spaceId(spaceId).titre("d").statut(ObjectifRevision.Statut.EN_COURS)
                .build();
        sansDate.setUpdatedAt(null);
        when(objectifRepository.findByUserIdAndSpaceId(userId, spaceId))
                .thenReturn(List.of(o1, o2, vieux, sansDate));

        WeeklyTrackingResponse res = objectifService.weeklyTracking(userId, spaceId);

        assertEquals(2, res.nbObjectifsAtteints());
        assertEquals(3, res.nbFichesGenerees());
        assertEquals(0.5, res.tauxProgression(), 1e-9);
        // lundi (o1 + suivi) + mardi (o2) = 2 jours distincts.
        assertEquals(2, res.joursActifs());
    }
}
