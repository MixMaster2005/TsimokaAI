package mg.esmia.miage.gamificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.gamificationservice.entity.Badge;
import mg.esmia.miage.gamificationservice.entity.BadgeObtenu;
import mg.esmia.miage.gamificationservice.entity.SuiviHebdomadaire;
import mg.esmia.miage.gamificationservice.repository.BadgeObtenuRepository;
import mg.esmia.miage.gamificationservice.repository.BadgeRepository;
import mg.esmia.miage.gamificationservice.repository.ObjectifRevisionRepository;
import mg.esmia.miage.gamificationservice.repository.RappelRepository;
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
    private final RappelRepository rappelRepository;

    /**
     * Idempotence : suivi hebdo verrouillé par UNIQUE(user_id, space_id, semaine_debut)
     * (getOrCreate) ; attribution des badges verrouillée par
     * {@code existsByUserIdAndBadgeId} + UNIQUE(user_id, badge_id) — une redélivrance
     * de FICHE_GENERATED ne duplique aucun badge. Le compteur hebdo nbFichesGenerees
     * est rejoué (+1) en cas de redélivrance (at-least-once).
     * TODO : déduplication via Set Redis clé ficheId
     * ({@code "gamification:dedup:fiche:<ficheId>"}, SETNX + TTL) pour rendre
     * l'incrément hebdo strictement idempotent.
     */
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

    /**
     * Impute le badge PREMIERE_FICHE_VALIDEE à l'auteur de la fiche ({@code userId},
     * événement FICHE_VALIDATED enrichi), pas au validateur. Idempotent via
     * {@code existsByUserIdAndBadgeId} + UNIQUE(user_id, badge_id) : une redélivrance
     * converge (no-op). Sans {@code userId} (contrat historique), no-op loggé.
     */
    @Transactional
    public void onFicheValidated(UUID userId, UUID enseignantId, String statut) {
        if (!"VALIDEE".equalsIgnoreCase(statut)) {
            return;
        }
        if (userId == null) {
            log.info("FICHE_VALIDATED (VALIDEE) reçu sans userId (contrat historique, "
                    + "enseignantId={}) — badge {} non imputable, en attente de "
                    + "l'événement enrichi.", enseignantId, BadgeCode.PREMIERE_FICHE_VALIDEE);
            return;
        }
        awardBadgeIfAbsent(userId, BadgeCode.PREMIERE_FICHE_VALIDEE);
    }

    /**
     * Contrat historique : l'événement ne portait que l'enseignantId.
     *
     * @deprecated Préférer {@link #onFicheValidated(UUID, UUID, String)} avec
     *             l'userId enrichi de la fiche.
     */
    @Deprecated
    @Transactional
    public void onFicheValidated(UUID enseignantId, String statut) {
        onFicheValidated(null, enseignantId, statut);
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
        rappelRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        objectifRepository.deleteByUserId(userId);
        suiviRepository.deleteByUserId(userId);
        badgeObtenuRepository.deleteByUserId(userId);
        rappelRepository.deleteByUserId(userId);
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
