package mg.esmia.miage.gamificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.gamificationservice.entity.Badge;
import mg.esmia.miage.gamificationservice.entity.BadgeObtenu;
import mg.esmia.miage.gamificationservice.entity.SuiviHebdomadaire;
import mg.esmia.miage.gamificationservice.repository.BadgeObtenuRepository;
import mg.esmia.miage.gamificationservice.repository.BadgeRepository;
import mg.esmia.miage.gamificationservice.repository.ObjectifRevisionRepository;
import mg.esmia.miage.gamificationservice.repository.SuiviHebdomadaireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/**
 * Attribution des badges + mise à jour du suivi hebdomadaire, déclenchées par
 * consommation de fiche.events. Implémentation COMPLETE (service générique).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final SuiviHebdomadaireRepository suiviRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeObtenuRepository badgeObtenuRepository;
    private final ObjectifRevisionRepository objectifRepository;

    @Transactional
    public void onFicheGenerated(UUID userId, UUID spaceId) {
        SuiviHebdomadaire suivi = getOrCreateSuiviCourant(userId, spaceId);
        suivi.setNbFichesGenerees(suivi.getNbFichesGenerees() + 1);
        suiviRepository.save(suivi);

        awardBadgeIfAbsent(userId, BadgeCode.PREMIERE_FICHE);

        // M7 : somme agrégée au lieu de charger toutes les lignes suivi
        long totalFiches = suiviRepository.sumNbFichesGenereesByUserIdAndSpaceId(userId, spaceId);
        if (totalFiches >= 5) {
            awardBadgeIfAbsent(userId, BadgeCode.CINQ_FICHES);
        }
    }

    @Transactional
    public void onFicheValidated(UUID enseignantId, String statut) {
        // NB : l'événement FICHE_VALIDATED (cf. contrat) porte enseignantId, pas l'étudiant
        // auteur de la fiche. Pour attribuer PREMIERE_FICHE_VALIDEE au bon étudiant, il
        // faudrait enrichir l'événement côté fiche-service avec l'userId de la fiche —
        // amélioration naturelle, non bloquante pour le fonctionnement du service.
        if ("VALIDEE".equalsIgnoreCase(statut)) {
            log.info("FICHE_VALIDATED (VALIDEE) reçu — attribution de {} à affiner une fois " +
                    "l'événement enrichi avec l'userId de l'auteur de la fiche.", BadgeCode.PREMIERE_FICHE_VALIDEE);
        }
    }

    @Transactional
    public void onObjectifAtteint(UUID userId, UUID spaceId) {
        SuiviHebdomadaire suivi = getOrCreateSuiviCourant(userId, spaceId);
        suivi.setNbObjectifsAtteints(suivi.getNbObjectifsAtteints() + 1);
        suiviRepository.save(suivi);
        awardBadgeIfAbsent(userId, BadgeCode.PREMIER_OBJECTIF_ATTEINT);
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        objectifRepository.deleteBySpaceId(spaceId);
        suiviRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        objectifRepository.deleteByUserId(userId);
        suiviRepository.deleteByUserId(userId);
        badgeObtenuRepository.deleteByUserId(userId);
    }

    private void awardBadgeIfAbsent(UUID userId, String badgeCode) {
        Badge badge = badgeRepository.findByCode(badgeCode).orElse(null);
        if (badge == null) {
            log.warn("Badge inconnu en base : {} (V2__seed_badges.sql non appliqué ?)", badgeCode);
            return;
        }
        if (!badgeObtenuRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
            badgeObtenuRepository.save(BadgeObtenu.builder().userId(userId).badgeId(badge.getId()).build());
            log.info("Badge '{}' attribué à l'utilisateur {}", badgeCode, userId);
        }
    }

    private SuiviHebdomadaire getOrCreateSuiviCourant(UUID userId, UUID spaceId) {
        LocalDate lundiCourant = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return suiviRepository.findByUserIdAndSpaceIdAndSemaineDebut(userId, spaceId, lundiCourant)
                .orElseGet(() -> SuiviHebdomadaire.builder()
                        .userId(userId).spaceId(spaceId).semaineDebut(lundiCourant).build());
    }
}
