package mg.esmia.miage.gamificationservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.gamificationservice.dto.CreateObjectifRequest;
import mg.esmia.miage.gamificationservice.dto.ObjectifResponse;
import mg.esmia.miage.gamificationservice.dto.UpdateObjectifRequest;
import mg.esmia.miage.gamificationservice.dto.WeeklyTrackingResponse;
import mg.esmia.miage.gamificationservice.entity.ObjectifRevision;
import mg.esmia.miage.gamificationservice.entity.SuiviHebdomadaire;
import mg.esmia.miage.gamificationservice.repository.ObjectifRevisionRepository;
import mg.esmia.miage.gamificationservice.repository.SuiviHebdomadaireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectifService {

    private final ObjectifRevisionRepository objectifRepository;
    private final GamificationService gamificationService;
    private final SuiviHebdomadaireRepository suiviRepository;

    @Transactional
    public ObjectifResponse create(UUID userId, CreateObjectifRequest request) {
        ObjectifRevision objectif = ObjectifRevision.builder()
                .userId(userId)
                .spaceId(request.spaceId())
                .titre(request.titre())
                .description(request.description())
                .dateEcheance(request.dateEcheance())
                .build();
        return ObjectifResponse.from(objectifRepository.save(objectif));
    }

    public List<ObjectifResponse> listMine(UUID userId, UUID spaceId) {
        return objectifRepository.findByUserIdAndSpaceId(userId, spaceId).stream().map(ObjectifResponse::from).toList();
    }

    /**
     * Récapitulatif de la semaine courante (Lot 2) — lecture seule, ne crée rien :
     * sans ligne de suivi ni objectifs, retourne des zéros (jamais 404) pour que le
     * front affiche un état vide honnête au lieu du placeholder.
     */
    @Transactional(readOnly = true)
    public WeeklyTrackingResponse weeklyTracking(UUID userId, UUID spaceId) {
        LocalDate lundi = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        SuiviHebdomadaire suivi = suiviRepository
                .findByUserIdAndSpaceIdAndSemaineDebut(userId, spaceId, lundi).orElse(null);
        int atteints = suivi == null || suivi.getNbObjectifsAtteints() == null
                ? 0 : suivi.getNbObjectifsAtteints();
        int fiches = suivi == null || suivi.getNbFichesGenerees() == null
                ? 0 : suivi.getNbFichesGenerees();

        List<ObjectifRevision> objectifs = objectifRepository.findByUserIdAndSpaceId(userId, spaceId);
        double taux = objectifs.isEmpty() ? 0.0 : Math.min(1.0, (double) atteints / objectifs.size());

        Instant debut = lundi.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fin = lundi.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
        Set<LocalDate> jours = new HashSet<>();
        for (ObjectifRevision o : objectifs) {
            if (dansSemaine(o.getUpdatedAt(), debut, fin)) {
                jours.add(o.getUpdatedAt().atZone(ZoneOffset.UTC).toLocalDate());
            }
        }
        if (suivi != null && (fiches > 0 || atteints > 0) && dansSemaine(suivi.getUpdatedAt(), debut, fin)) {
            jours.add(suivi.getUpdatedAt().atZone(ZoneOffset.UTC).toLocalDate());
        }
        return new WeeklyTrackingResponse(lundi.toString(), atteints, fiches, taux, jours.size());
    }

    private static boolean dansSemaine(Instant instant, Instant debut, Instant fin) {
        return instant != null && !instant.isBefore(debut) && instant.isBefore(fin);
    }

    @Transactional
    public ObjectifResponse updateStatut(UUID id, UUID requesterId, UpdateObjectifRequest request) {
        ObjectifRevision objectif = objectifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objectif introuvable : " + id));
        if (!objectif.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à cet objectif");
        }
        objectif.setStatut(request.statut());
        objectif = objectifRepository.save(objectif);

        if (request.statut() == ObjectifRevision.Statut.ATTEINT) {
            gamificationService.onObjectifAtteint(requesterId, objectif.getSpaceId());
        }
        return ObjectifResponse.from(objectif);
    }
}
