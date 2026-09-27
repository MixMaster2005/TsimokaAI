package mg.esmia.miage.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Tâches planifiées des recommandations (Lot 1).
 *
 * <ul>
 *   <li>Quotidien 08:00 UTC : relance des étudiants inactifs depuis 7 jours
 *   ({@code RELANCE_INACTIVITE}, anti-doublon 7 j).</li>
 *   <li>Dimanche 03:00 UTC : purge des recommandations de plus de 30 jours.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationMaintenanceService {

    private final AnalyticsService analyticsService;

    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    public void relanceQuotidienne() {
        try {
            int generes = analyticsService.relancerInactifs();
            log.info("Maintenance recommandations : {} relance(s) inactivité.", generes);
        } catch (Exception e) {
            log.error("Échec relance inactivité", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * SUN", zone = "UTC")
    public void purgeHebdomadaire() {
        try {
            analyticsService.purgerAnciennesRecommandations();
            log.info("Maintenance recommandations : purge > 30 j effectuée.");
        } catch (Exception e) {
            log.error("Échec purge recommandations", e);
        }
    }
}
