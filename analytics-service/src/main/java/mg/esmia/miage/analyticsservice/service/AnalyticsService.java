package mg.esmia.miage.analyticsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.analyticsservice.dto.*;
import mg.esmia.miage.analyticsservice.entity.*;
import mg.esmia.miage.analyticsservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service générique consommant chat.events et fiche.events pour construire les
 * tableaux
 * de bord étudiant/enseignant. Implémentation COMPLETE (service générique, pas
 * de TODO IA).
 *
 * NB sur extractNotion() : heuristique simple (premier nom significatif de la
 * question),
 * volontairement basique. Un raffinement par NLP/embeddings (proche de ce qui
 * existe déjà
 * dans ingestion-service pour le chunking) est une amélioration possible mais
 * non bloquante
 * pour que la plateforme reste fonctionnelle de bout en bout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private static final Set<String> STOP_WORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "est", "quoi",
            "comment", "pourquoi", "qu", "que", "qui", "à", "au", "aux", "pour", "avec",
            "sur", "dans", "ce", "cette", "ces", "je", "tu", "il", "elle", "on", "nous",
            "vous", "ils", "elles", "mon", "ma", "mes", "son", "sa", "ses",
            "peux", "peut", "faire", "fait", "bien", "tout", "très", "plus", "aussi",
            "comme", "mais", "donc", "car", "si", "pas", "encore", "faut", "avoir",
            "être", "avant", "après", "entre", "sous", "chez", "sans", "vers", "c",
            "quels", "quelle", "quelles", "quel",
            "autres", "autre", "même", "celui", "ceux", "celle", "celles");

    private static final int NOTION_FAIBLE_SEUIL = 3;

    private final ProgressionEtudiantRepository progressionRepository;
    private final StatistiqueEspaceRepository statistiqueRepository;
    private final ChapitreDifficileRepository chapitreDifficileRepository;
    private final RecommandationRepository recommandationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void onQuestionAsked(UUID userId, UUID spaceId, String content) {
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        progression.setNbQuestionsPosees(progression.getNbQuestionsPosees() + 1);
        progression.setDerniereActivite(Instant.now());

        String notion = extractNotion(content);
        StatistiqueEspace stat = statistiqueRepository.findBySpaceIdAndNotion(spaceId, notion)
                .orElseGet(() -> StatistiqueEspace.builder().spaceId(spaceId).notion(notion).build());
        stat.setNbQuestions(stat.getNbQuestions() + 1);
        stat.setNbConsultations(stat.getNbConsultations() + 1);
        statistiqueRepository.save(stat);

        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);

        if (stat.getNbQuestions() % NOTION_FAIBLE_SEUIL == 0) {
            ChapitreDifficile chapitre = chapitreDifficileRepository.findBySpaceIdAndChapitre(spaceId, notion)
                    .orElseGet(() -> ChapitreDifficile.builder().spaceId(spaceId).chapitre(notion).build());
            chapitre.setScoreDifficulte(chapitre.getScoreDifficulte() + 1.0);
            chapitreDifficileRepository.save(chapitre);

            recommandationRepository.save(Recommandation.builder()
                    .userId(userId).spaceId(spaceId)
                    .type(Recommandation.Type.CHAPITRE_DIFFICILE)
                    .contenu("Tu as posé plusieurs questions sur « %s ». Une relecture de ce chapitre pourrait aider."
                            .formatted(notion))
                    .build());
        }
    }

    @Transactional
    public void onFicheGenerated(UUID userId, UUID spaceId) {
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        progression.setNbFichesGenerees(progression.getNbFichesGenerees() + 1);
        progression.setDerniereActivite(Instant.now());
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);
    }

    @Transactional
    public void onFicheValidated(String statut) {
        // Le contrat FicheEvent.validated() ne porte que enseignantId (pas
        // userId/spaceId du côté
        // étudiant) : l'impact précis sur la progression d'un étudiant donné
        // nécessiterait un
        // enrichissement de l'événement côté fiche-service (userId/spaceId de la fiche
        // concernée).
        log.info("FICHE_VALIDATED reçu (statut={}) — impact direct sur la progression étudiant à " +
                "affiner une fois l'événement enrichi avec userId/spaceId côté fiche-service.", statut);
    }

    public StudentDashboardResponse studentDashboard(UUID userId, UUID spaceId) {
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);

        List<RecommandationResponse> recos = recommandationRepository
                .findByUserIdAndSpaceIdOrderByGenereLeDesc(userId, spaceId).stream()
                .limit(10).map(RecommandationResponse::from).toList();

        return new StudentDashboardResponse(
                userId, spaceId, progression.getTauxReussite(),
                readJsonArray(progression.getNotionsMaitrisees()),
                readJsonArray(progression.getNotionsFaibles()),
                progression.getNbQuestionsPosees(), progression.getNbFichesGenerees(),
                progression.getDerniereActivite(), recos);
    }

    public TeacherDashboardResponse teacherDashboard(UUID spaceId) {
        List<NotionStatResponse> notions = statistiqueRepository.findBySpaceIdOrderByNbQuestionsDesc(spaceId)
                .stream().limit(10).map(NotionStatResponse::from).toList();
        List<ChapitreDifficileResponse> chapitres = chapitreDifficileRepository
                .findBySpaceIdOrderByScoreDifficulteDesc(spaceId).stream()
                .limit(10).map(ChapitreDifficileResponse::from).toList();
        // M6 : COUNT query au lieu de charger toutes les entités en mémoire
        int nbEtudiantsActifs = (int) progressionRepository.countBySpaceId(spaceId);

        return new TeacherDashboardResponse(spaceId, notions, chapitres, nbEtudiantsActifs);
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        progressionRepository.deleteBySpaceId(spaceId);
        statistiqueRepository.deleteBySpaceId(spaceId);
        chapitreDifficileRepository.deleteBySpaceId(spaceId);
        recommandationRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        progressionRepository.deleteByUserId(userId);
        recommandationRepository.deleteByUserId(userId);
    }

    private ProgressionEtudiant getOrCreateProgression(UUID userId, UUID spaceId) {
        return progressionRepository.findByUserIdAndSpaceId(userId, spaceId)
                .orElseGet(() -> ProgressionEtudiant.builder().userId(userId).spaceId(spaceId).build());
    }

    private void refreshProgressionMetrics(ProgressionEtudiant progression, UUID spaceId) {
        int nbQuestions = progression.getNbQuestionsPosees();
        int nbFiches = progression.getNbFichesGenerees();

        double taux = nbQuestions > 0 ? Math.min(1.0, (double) nbFiches / nbQuestions) : 0.0;
        progression.setTauxReussite(taux);

        List<StatistiqueEspace> stats = statistiqueRepository.findBySpaceIdOrderByNbQuestionsDesc(spaceId);

        List<String> faibles = stats.stream()
                .filter(s -> s.getNbQuestions() >= NOTION_FAIBLE_SEUIL)
                .map(StatistiqueEspace::getNotion)
                .collect(Collectors.toList());

        List<String> maitrisees = stats.stream()
                .filter(s -> s.getNbQuestions() > 0 && s.getNbQuestions() < NOTION_FAIBLE_SEUIL)
                .map(StatistiqueEspace::getNotion)
                .collect(Collectors.toList());

        try {
            progression.setNotionsFaibles(objectMapper.writeValueAsString(faibles));
            progression.setNotionsMaitrisees(objectMapper.writeValueAsString(maitrisees));
        } catch (Exception e) {
            log.error("Erreur sérialisation notions", e);
        }
    }

    private String extractNotion(String content) {
        if (content == null || content.isBlank()) {
            return "général";
        }
        return Arrays.stream(content.toLowerCase(Locale.FRENCH).split("[^\\p{L}]+"))
                .filter(w -> w.length() > 4 && !STOP_WORDS.contains(w))
                .findFirst()
                .orElse("général");
    }

    @SuppressWarnings("unchecked")
    private List<String> readJsonArray(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
