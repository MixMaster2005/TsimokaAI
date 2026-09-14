package mg.esmia.miage.analyticsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.analyticsservice.dto.*;
import mg.esmia.miage.analyticsservice.entity.*;
import mg.esmia.miage.analyticsservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

    /** Longueur max d'une question normalisée persistée (cf. question_frequente). */
    private static final int QUESTION_MAX_LENGTH = 280;

    /** Seuil d'alerte quiz : score < 50% répété. */
    private static final double QUIZ_DIFFICULTE_SEUIL = 50.0;

    /** Nombre de semaines couvertes par le champ evolution du dashboard enseignant. */
    private static final int EVOLUTION_NB_SEMAINES = 12;

    private final ProgressionEtudiantRepository progressionRepository;
    private final StatistiqueEspaceRepository statistiqueRepository;
    private final ChapitreDifficileRepository chapitreDifficileRepository;
    private final RecommandationRepository recommandationRepository;
    private final QuestionFrequenteRepository questionFrequenteRepository;
    private final ObjectMapper objectMapper;

    /**
     * Idempotence : ligne progression verrouillée par UNIQUE(user_id, space_id)
     * (getOrCreate) et question agrégée par UNIQUE(space_id, question_normalisee)
     * (upsert). Redis Pub/Sub est at-least-once : une redélivrance de MESSAGE_CREATED
     * rejoue les compteurs (+1). TODO : déduplication via Set Redis clé messageId
     * (clé {@code "analytics:dedup:message:<messageId>"}, SETNX + TTL) quand le
     * producteur propagera un identifiant stable.
     */
    @Transactional
    public void onQuestionAsked(UUID userId, UUID spaceId, String content) {
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        progression.setNbQuestionsPosees(progression.getNbQuestionsPosees() + 1);
        progression.setDerniereActivite(Instant.now());

        upsertQuestionFrequente(spaceId, content);

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

    /**
     * Idempotence : getOrCreate adossé à UNIQUE(user_id, space_id) — pas de ligne
     * dupliquée. Compteur nbFichesGenerees rejoué en cas de redélivrance
     * (at-least-once). TODO : garde-fou {@code exists} / Set Redis clé ficheId
     * ({@code "analytics:dedup:fiche:<ficheId>"}) si le producteur joint l'ID.
     */
    @Transactional
    public void onFicheGenerated(UUID userId, UUID spaceId) {
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        progression.setNbFichesGenerees(progression.getNbFichesGenerees() + 1);
        progression.setDerniereActivite(Instant.now());
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);
    }

    @Transactional
    public void onFicheValidated(UUID userId, UUID spaceId, String statut) {
        // Contrat historique : FicheEvent.validated() ne portait que enseignantId
        // (userId/spaceId nulls). Avec l'événement enrichi on crédite la progression
        // de l'étudiant concerné ; sans userId/spaceId on se contente de logger.
        if (userId == null || spaceId == null) {
            log.info("FICHE_VALIDATED reçu (statut={}) sans userId/spaceId — "
                    + "progression non imputable, en attente de l'événement enrichi.", statut);
            return;
        }
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        progression.setDerniereActivite(Instant.now());
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);
        log.info("FICHE_VALIDATED (statut={}) crédité à la progression userId={} spaceId={}.",
                statut, userId, spaceId);
    }

    /**
     * Surcharge historique (événement non enrichi) : conservée pour compatibilité,
     * délègue vers la version enrichie en no-op loggé.
     */
    @Transactional
    public void onFicheValidated(String statut) {
        onFicheValidated(null, null, statut);
    }

    /**
     * Idempotence : getOrCreate adossé à UNIQUE(user_id, space_id). Une redélivrance
     * de QUIZ_SUBMITTED rejoue nbQuizPasses (+1) et écrase dernier/meilleur score
     * avec les mêmes valeurs (effet convergent). TODO : déduplication via Set Redis
     * clé attemptId ({@code "analytics:dedup:attempt:<attemptId>"}) quand le payload
     * le porte systématiquement.
     */
    @Transactional
    public void onQuizSubmitted(UUID spaceId, UUID userId, double score, double total) {
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        double pct = toPourcentage(score, total);
        Double precedent = progression.getDernierScore();

        progression.setNbQuizPasses(nullSafe(progression.getNbQuizPasses()) + 1);
        progression.setDernierScore(pct);
        if (progression.getMeilleurScore() == null || pct > progression.getMeilleurScore()) {
            progression.setMeilleurScore(pct);
        }
        progression.setDerniereActivite(Instant.now());
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);

        maybeGenererRecommandationQuizDifficile(progression, precedent, pct);
    }

    /**
     * Idempotence par construction : rejoue les scores sans incrémenter
     * nbQuizPasses — une redélivrance de QUIZ_CORRECTED converge vers les mêmes
     * valeurs (dernier/meilleur score écrasés à l'identique).
     */
    @Transactional
    public void onQuizCorrected(UUID spaceId, UUID userId, double scoreCorrige, double total) {
        // Une correction ne rejoue pas le quiz : pas d'incrément de nbQuizPasses,
        // seuls les scores (et l'activité) sont mis à jour.
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        double pct = toPourcentage(scoreCorrige, total);
        Double precedent = progression.getDernierScore();

        progression.setDernierScore(pct);
        if (progression.getMeilleurScore() == null || pct > progression.getMeilleurScore()) {
            progression.setMeilleurScore(pct);
        }
        progression.setDerniereActivite(Instant.now());
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);

        maybeGenererRecommandationQuizDifficile(progression, precedent, pct);
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
        List<QuestionFrequenteResponse> questionsFrequentes = questionFrequenteRepository
                .findTop10BySpaceIdOrderByNbOccurrencesDesc(spaceId).stream()
                .map(QuestionFrequenteResponse::from).toList();
        List<EvolutionSemaineResponse> evolution = evolution12Semaines(spaceId);

        return new TeacherDashboardResponse(spaceId, notions, chapitres, nbEtudiantsActifs,
                questionsFrequentes, evolution);
    }

    public List<StudentRowResponse> teacherStudents(UUID spaceId) {
        return progressionRepository.findBySpaceId(spaceId).stream()
                .map(StudentRowResponse::from).toList();
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        progressionRepository.deleteBySpaceId(spaceId);
        statistiqueRepository.deleteBySpaceId(spaceId);
        chapitreDifficileRepository.deleteBySpaceId(spaceId);
        recommandationRepository.deleteBySpaceId(spaceId);
        questionFrequenteRepository.deleteBySpaceId(spaceId);
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

    /**
     * Persiste la question brute normalisée (upsert par spaceId+questionNormalisee).
     * Les questions vides après normalisation sont ignorées.
     */
    private void upsertQuestionFrequente(UUID spaceId, String content) {
        String normalisee = normaliseQuestion(content);
        if (normalisee.isBlank()) {
            return;
        }
        QuestionFrequente qf = questionFrequenteRepository
                .findBySpaceIdAndQuestionNormalisee(spaceId, normalisee)
                .orElseGet(() -> QuestionFrequente.builder()
                        .spaceId(spaceId).questionNormalisee(normalisee).nbOccurrences(0).build());
        qf.setNbOccurrences(nullSafe(qf.getNbOccurrences()) + 1);
        qf.setDernierAsk(Instant.now());
        questionFrequenteRepository.save(qf);
    }

    /** Normalisation : lowercase/trim/collapse des espaces, tronquée à 280 car. */
    static String normaliseQuestion(String content) {
        if (content == null) {
            return "";
        }
        String normalisee = content.toLowerCase(Locale.FRENCH).trim().replaceAll("\\s+", " ");
        if (normalisee.length() > QUESTION_MAX_LENGTH) {
            normalisee = normalisee.substring(0, QUESTION_MAX_LENGTH);
        }
        return normalisee;
    }

    /**
     * Évolution de l'activité sur les 12 dernières semaines (semaine en cours incluse) :
     * pour chaque semaine, nb d'étudiants dont derniereActivite tombe dans la semaine.
     * Agrégation en Java (pas de native query) à partir des progressions de l'espace.
     */
    private List<EvolutionSemaineResponse> evolution12Semaines(UUID spaceId) {
        List<ProgressionEtudiant> progressions = progressionRepository.findBySpaceId(spaceId);
        LocalDate lundiCourant = LocalDate.now(ZoneOffset.UTC).with(DayOfWeek.MONDAY);
        List<EvolutionSemaineResponse> evolution = new ArrayList<>(EVOLUTION_NB_SEMAINES);
        for (int i = EVOLUTION_NB_SEMAINES - 1; i >= 0; i--) {
            LocalDate debutSemaine = lundiCourant.minusWeeks(i);
            Instant debut = debutSemaine.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant fin = debutSemaine.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
            int nbActifs = (int) progressions.stream()
                    .filter(p -> p.getDerniereActivite() != null
                            && !p.getDerniereActivite().isBefore(debut)
                            && p.getDerniereActivite().isBefore(fin))
                    .count();
            evolution.add(new EvolutionSemaineResponse(debutSemaine.toString(), nbActifs));
        }
        return evolution;
    }

    private static double toPourcentage(double score, double total) {
        if (total <= 0) {
            return 0.0;
        }
        return (score / total) * 100.0;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Génère une recommandation CHAPITRE_DIFFICILE quand un score < 50% est répété
     * (le score précédent était lui aussi < 50%, ou l'étudiant cumule déjà
     * plusieurs quiz avec un meilleur score < 50%).
     */
    private void maybeGenererRecommandationQuizDifficile(ProgressionEtudiant progression,
                                                         Double scorePrecedent, double pct) {
        if (pct >= QUIZ_DIFFICULTE_SEUIL) {
            return;
        }
        boolean precedentFaible = scorePrecedent != null && scorePrecedent < QUIZ_DIFFICULTE_SEUIL;
        boolean historiqueFaible = nullSafe(progression.getNbQuizPasses()) >= 2
                && progression.getMeilleurScore() != null
                && progression.getMeilleurScore() < QUIZ_DIFFICULTE_SEUIL;
        if (precedentFaible || historiqueFaible) {
            recommandationRepository.save(Recommandation.builder()
                    .userId(progression.getUserId()).spaceId(progression.getSpaceId())
                    .type(Recommandation.Type.CHAPITRE_DIFFICILE)
                    .contenu("Tes derniers quiz sont sous les 50%% (dernier : %.1f%%). "
                            .formatted(pct)
                            + "Revois les chapitres récents et retente un quiz pour valider.")
                    .build());
        }
    }

    private String extractNotion(String content) {        if (content == null || content.isBlank()) {
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
