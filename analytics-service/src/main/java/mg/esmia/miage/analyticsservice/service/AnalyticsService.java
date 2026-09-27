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
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
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

    /** Longueur max d'une clé de notion persistée (cf. statistique_* .notion VARCHAR(255)). */
    private static final int NOTION_CLE_MAX_LENGTH = 60;

    /** Valeur d'affichage quand aucune notion significative n'est extraite. */
    private static final String NOTION_GENERALE = "général";

    /** Longueur max d'une question normalisée persistée (cf. question_frequente). */
    private static final int QUESTION_MAX_LENGTH = 280;

    /** Seuil d'alerte quiz : score < 50% répété. */
    private static final double QUIZ_DIFFICULTE_SEUIL = 50.0;

    /** Anti-doublon recommandations quiz/questions répétées : 24 h sur même contenu. */
    private static final long RECO_ANTI_DOUBLON_HEURES = 24;

    /** Seuil d'inactivité déclenchant RELANCE_INACTIVITE : 7 jours sans activité. */
    private static final long RELANCE_INACTIVITE_JOURS = 7;

    /** Rétention des recommandations : purge au-delà de 30 jours. */
    private static final long RECO_RETENTION_JOURS = 30;

    /** Nombre de semaines couvertes par le champ evolution du dashboard enseignant. */
    private static final int EVOLUTION_NB_SEMAINES = 12;

    private final ProgressionEtudiantRepository progressionRepository;
    private final StatistiqueEspaceRepository statistiqueRepository;
    private final StatistiqueNotionUserRepository notionUserRepository;
    private final ActiviteJournaliereRepository activiteRepository;
    private final ChapitreDifficileRepository chapitreDifficileRepository;
    private final RecommandationRepository recommandationRepository;
    private final QuestionFrequenteRepository questionFrequenteRepository;
    private final EventDedupService dedupService;
    private final ObjectMapper objectMapper;

    /**
     * Idempotence (Lot 3) : déduplication Redis SETNX+TTL sur {@code messageId}
     * (clé {@code analytics:dedup:message:<messageId>}) — une redélivrance de
     * MESSAGE_CREATED est ignorée au lieu de rejouer les compteurs (+1).
     * Sans messageId (contrat historique), traitement sans déduplication.
     */
    @Transactional
    public void onQuestionAsked(UUID userId, UUID spaceId, String content, String messageId) {
        if (messageId != null && !messageId.isBlank()
                && !dedupService.tryMarkProcessed("analytics:dedup:message:" + messageId)) {
            log.info("MESSAGE_CREATED déjà traité ignoré (messageId={}).", messageId);
            return;
        }
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        progression.setNbQuestionsPosees(progression.getNbQuestionsPosees() + 1);
        progression.setDerniereActivite(Instant.now());

        upsertQuestionFrequente(spaceId, content);

        // Clé de notion : extraction lexicale améliorée (jusqu'à 2 mots) avec repli
        // sur la question normalisée tronquée pour éviter la clé fourre-tout "général".
        String notion = notionKey(content);
        StatistiqueEspace stat = statistiqueRepository.findBySpaceIdAndNotion(spaceId, notion)
                .orElseGet(() -> StatistiqueEspace.builder().spaceId(spaceId).notion(notion).build());
        stat.setNbQuestions(stat.getNbQuestions() + 1);
        stat.setNbConsultations(stat.getNbConsultations() + 1);
        statistiqueRepository.save(stat);

        // Compteur personnel (dashboard étudiant) — l'agrégat global ci-dessus reste
        // réservé au dashboard enseignant.
        StatistiqueNotionUser statUser = notionUserRepository
                .findByUserIdAndSpaceIdAndNotion(userId, spaceId, notion)
                .orElseGet(() -> StatistiqueNotionUser.builder()
                        .userId(userId).spaceId(spaceId).notion(notion).build());
        statUser.setNbQuestions(nullSafe(statUser.getNbQuestions()) + 1);
        notionUserRepository.save(statUser);

        enregistrerActivite(userId, spaceId, TypeActivite.QUESTION);

        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);

        if (statUser.getNbQuestions() % NOTION_FAIBLE_SEUIL == 0) {
            ChapitreDifficile chapitre = chapitreDifficileRepository.findBySpaceIdAndChapitre(spaceId, notion)
                    .orElseGet(() -> ChapitreDifficile.builder().spaceId(spaceId).chapitre(notion).build());
            chapitre.setScoreDifficulte(chapitre.getScoreDifficulte() + 1.0);
            chapitreDifficileRepository.save(chapitre);

            genererRecommandation(userId, spaceId, Recommandation.Type.CHAPITRE_DIFFICILE,
                    "Tu as posé plusieurs questions sur « %s ». Une relecture de ce chapitre pourrait aider."
                            .formatted(notion),
                    RECO_ANTI_DOUBLON_HEURES);
        }
    }

    /**
     * Surcharge historique sans identifiant stable : conservée pour compatibilité,
     * traite sans déduplication.
     */
    @Transactional
    public void onQuestionAsked(UUID userId, UUID spaceId, String content) {
        onQuestionAsked(userId, spaceId, content, null);
    }

    /**
     * Idempotence (Lot 3) : déduplication Redis sur {@code ficheId}
     * (clé {@code analytics:dedup:fiche:<ficheId>}). Sans ficheId, sans déduplication.
     */
    @Transactional
    public void onFicheGenerated(UUID userId, UUID spaceId, String ficheId) {
        if (ficheId != null && !ficheId.isBlank()
                && !dedupService.tryMarkProcessed("analytics:dedup:fiche:" + ficheId)) {
            log.info("FICHE_GENERATED déjà traité ignoré (ficheId={}).", ficheId);
            return;
        }
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        progression.setNbFichesGenerees(progression.getNbFichesGenerees() + 1);
        progression.setDerniereActivite(Instant.now());
        enregistrerActivite(userId, spaceId, TypeActivite.FICHE);
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);
    }

    /**
     * Surcharge historique sans identifiant stable : conservée pour compatibilité,
     * traite sans déduplication.
     */
    @Transactional
    public void onFicheGenerated(UUID userId, UUID spaceId) {
        onFicheGenerated(userId, spaceId, null);
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
        enregistrerActivite(userId, spaceId, TypeActivite.AUTRE);
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
     * Idempotence (Lot 3) : déduplication Redis sur {@code attemptId}
     * (clé {@code analytics:dedup:attempt:<attemptId>}) — une redélivrance de
     * QUIZ_SUBMITTED n'incrémente plus nbQuizPasses. Sans attemptId (contrat
     * historique), effet convergent comme avant (scores écrasés à l'identique).
     */
    @Transactional
    public void onQuizSubmitted(UUID spaceId, UUID userId, double score, double total, String attemptId) {
        if (attemptId != null && !attemptId.isBlank()
                && !dedupService.tryMarkProcessed("analytics:dedup:attempt:" + attemptId)) {
            log.info("QUIZ_SUBMITTED déjà traité ignoré (attemptId={}).", attemptId);
            return;
        }
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        double pct = toPourcentage(score, total);
        Double precedent = progression.getDernierScore();

        progression.setNbQuizPasses(nullSafe(progression.getNbQuizPasses()) + 1);
        progression.setDernierScore(pct);
        if (progression.getMeilleurScore() == null || pct > progression.getMeilleurScore()) {
            progression.setMeilleurScore(pct);
        }
        progression.setDerniereActivite(Instant.now());
        enregistrerActivite(userId, spaceId, TypeActivite.QUIZ);
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);

        maybeGenererRecommandationQuizDifficile(progression, precedent, pct);
    }

    /**
     * Surcharge historique sans identifiant stable : conservée pour compatibilité,
     * traite sans déduplication (effet convergent sur les scores).
     */
    @Transactional
    public void onQuizSubmitted(UUID spaceId, UUID userId, double score, double total) {
        onQuizSubmitted(spaceId, userId, score, total, null);
    }

    /**
     * Idempotence par construction : rejoue les scores sans incrémenter
     * nbQuizPasses — une redélivrance de QUIZ_CORRECTED converge vers les mêmes
     * valeurs (dernier/meilleur score écrasés à l'identique, reco couverte par
     * l'anti-doublon 24 h). L'activité du jour est réenregistrée (idempotent).
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
        enregistrerActivite(userId, spaceId, TypeActivite.QUIZ);
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);

        maybeGenererRecommandationQuizDifficile(progression, precedent, pct);
    }

    public StudentDashboardResponse studentDashboard(UUID userId, UUID spaceId) {
        ProgressionEtudiant progression = getOrCreateProgression(userId, spaceId);
        refreshProgressionMetrics(progression, spaceId);
        progressionRepository.save(progression);
        return toStudentDashboard(progression);
    }

    /**
     * Vue transverse (Lot 3) : un dashboard par espace où l'étudiant a une progression,
     * pour remplacer les N requêtes parallèles du front ({@code useAllTauxReussite}).
     */
    public List<StudentDashboardResponse> allStudentDashboards(UUID userId) {
        return progressionRepository.findByUserId(userId).stream()
                .map(p -> {
                    refreshProgressionMetrics(p, p.getSpaceId());
                    progressionRepository.save(p);
                    return toStudentDashboard(p);
                })
                .toList();
    }

    private StudentDashboardResponse toStudentDashboard(ProgressionEtudiant progression) {
        List<RecommandationResponse> recos = recommandationRepository
                .findByUserIdAndSpaceIdOrderByGenereLeDesc(
                        progression.getUserId(), progression.getSpaceId()).stream()
                .limit(10).map(RecommandationResponse::from).toList();

        return new StudentDashboardResponse(
                progression.getUserId(), progression.getSpaceId(), progression.getTauxReussite(),
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
        notionUserRepository.deleteBySpaceId(spaceId);
        activiteRepository.deleteBySpaceId(spaceId);
        chapitreDifficileRepository.deleteBySpaceId(spaceId);
        recommandationRepository.deleteBySpaceId(spaceId);
        questionFrequenteRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        progressionRepository.deleteByUserId(userId);
        notionUserRepository.deleteByUserId(userId);
        activiteRepository.deleteByUserId(userId);
        recommandationRepository.deleteByUserId(userId);
    }

    private ProgressionEtudiant getOrCreateProgression(UUID userId, UUID spaceId) {
        return progressionRepository.findByUserIdAndSpaceId(userId, spaceId)
                .orElseGet(() -> ProgressionEtudiant.builder().userId(userId).spaceId(spaceId).build());
    }

    private void refreshProgressionMetrics(ProgressionEtudiant progression, UUID spaceId) {
        progression.setTauxReussite(calculerTauxReussite(
                progression.getNbQuestionsPosees(),
                progression.getNbFichesGenerees(),
                progression.getNbQuizPasses(),
                progression.getMeilleurScore(),
                progression.getDernierScore()));

        // Notions personnelles (par étudiant) — l'agrégat global statistique_espace
        // reste réservé au dashboard enseignant.
        List<StatistiqueNotionUser> stats = notionUserRepository
                .findByUserIdAndSpaceIdOrderByNbQuestionsDesc(progression.getUserId(), spaceId);

        List<String> faibles = stats.stream()
                .filter(s -> nullSafe(s.getNbQuestions()) >= NOTION_FAIBLE_SEUIL)
                .map(StatistiqueNotionUser::getNotion)
                .collect(Collectors.toList());

        List<String> maitrisees = stats.stream()
                .filter(s -> nullSafe(s.getNbQuestions()) > 0
                        && nullSafe(s.getNbQuestions()) < NOTION_FAIBLE_SEUIL)
                .map(StatistiqueNotionUser::getNotion)
                .collect(Collectors.toList());

        try {
            progression.setNotionsFaibles(objectMapper.writeValueAsString(faibles));
            progression.setNotionsMaitrisees(objectMapper.writeValueAsString(maitrisees));
        } catch (Exception e) {
            log.error("Erreur sérialisation notions", e);
        }
    }

    /**
     * Touche le journal d'activité du jour UTC (Lot 3) : upsert
     * {@code UNIQUE(user_id, space_id, jour)} + incrément du compteur du type
     * d'événement. Idempotent par construction (pas de double ligne possible).
     */
    private void enregistrerActivite(UUID userId, UUID spaceId, TypeActivite type) {
        LocalDate jour = LocalDate.now(ZoneOffset.UTC);
        ActiviteJournaliere activite = activiteRepository
                .findByUserIdAndSpaceIdAndJour(userId, spaceId, jour)
                .orElseGet(() -> ActiviteJournaliere.builder()
                        .userId(userId).spaceId(spaceId).jour(jour).build());
        switch (type) {
            case QUESTION -> activite.setNbQuestions(nullSafe(activite.getNbQuestions()) + 1);
            case FICHE -> activite.setNbFiches(nullSafe(activite.getNbFiches()) + 1);
            case QUIZ -> activite.setNbQuiz(nullSafe(activite.getNbQuiz()) + 1);
            case AUTRE -> { /* simple présence du jour */ }
        }
        activiteRepository.save(activite);
    }

    private enum TypeActivite {
        QUESTION, FICHE, QUIZ, AUTRE
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
     * Évolution de l'activité sur les 12 dernières semaines (semaine en cours incluse,
     * Lot 3) : pour chaque semaine, nb d'étudiants DISTINCTS actifs d'après le journal
     * {@code activite_journaliere}. Repli sur l'ancienne heuristique
     * ({@code derniereActivite}) quand le journal est encore vide (données
     * pré-migration).
     */
    private List<EvolutionSemaineResponse> evolution12Semaines(UUID spaceId) {
        LocalDate lundiCourant = LocalDate.now(ZoneOffset.UTC).with(DayOfWeek.MONDAY);
        LocalDate debutFenetre = lundiCourant.minusWeeks(EVOLUTION_NB_SEMAINES - 1);
        List<ActiviteJournaliere> journal =
                activiteRepository.findBySpaceIdAndJourBetween(spaceId, debutFenetre, lundiCourant.plusDays(6));
        if (!journal.isEmpty()) {
            List<EvolutionSemaineResponse> evolution = new ArrayList<>(EVOLUTION_NB_SEMAINES);
            for (int i = EVOLUTION_NB_SEMAINES - 1; i >= 0; i--) {
                LocalDate debutSemaine = lundiCourant.minusWeeks(i);
                LocalDate finSemaine = debutSemaine.plusDays(6);
                long nbActifs = journal.stream()
                        .filter(a -> !a.getJour().isBefore(debutSemaine) && !a.getJour().isAfter(finSemaine))
                        .map(ActiviteJournaliere::getUserId)
                        .distinct()
                        .count();
                evolution.add(new EvolutionSemaineResponse(debutSemaine.toString(), (int) nbActifs));
            }
            return evolution;
        }
        return evolutionLegacy(spaceId, lundiCourant);
    }

    /** Ancienne heuristique (pré-Lot 3) : conservée en repli sans journal. */
    private List<EvolutionSemaineResponse> evolutionLegacy(UUID spaceId, LocalDate lundiCourant) {
        List<ProgressionEtudiant> progressions = progressionRepository.findBySpaceId(spaceId);
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

    /**
     * Taux de réussite 0.0-1.0 (Lot 1) : si l'étudiant a passé des quiz, le taux reflète
     * sa performance (meilleur score, repli dernier score) ; sinon repli sur l'ancien
     * ratio fiches/questions. Évite l'indicateur mensonger à 10 % pour un étudiant à
     * 90 % aux quiz mais peu de fiches.
     */
    static double calculerTauxReussite(Integer nbQuestions, Integer nbFiches,
                                       Integer nbQuizPasses, Double meilleurScore, Double dernierScore) {
        int nbQuiz = nbQuizPasses == null ? 0 : nbQuizPasses;
        if (nbQuiz > 0) {
            Double ref = meilleurScore != null ? meilleurScore : dernierScore;
            if (ref != null) {
                return Math.min(1.0, Math.max(0.0, ref / 100.0));
            }
            return 0.0;
        }
        int questions = nbQuestions == null ? 0 : nbQuestions;
        int fiches = nbFiches == null ? 0 : nbFiches;
        return questions > 0 ? Math.min(1.0, (double) fiches / questions) : 0.0;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Génère une recommandation REVISION_NOTION_FAIBLE quand un score &lt; 50% est répété
     * (le score précédent était lui aussi &lt; 50%, ou l'étudiant cumule déjà
     * plusieurs quiz avec un meilleur score &lt; 50%).
     *
     * <p>Note Lot 1 : type dédié (anciennement CHAPITRE_DIFFICILE par erreur) pour
     * distinguer « questions répétées sur un chapitre » (CHAPITRE_DIFFICILE) de
     * « échecs répétés aux quiz » (REVISION_NOTION_FAIBLE). Anti-doublon 24 h.
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
            genererRecommandation(
                    progression.getUserId(), progression.getSpaceId(),
                    Recommandation.Type.REVISION_NOTION_FAIBLE,
                    "Tes derniers quiz sont sous les 50%% (dernier : %.1f%%). "
                            .formatted(pct)
                            + "Revois les chapitres récents et retente un quiz pour valider.",
                    RECO_ANTI_DOUBLON_HEURES);
        }
    }

    /**
     * Relance les étudiants inactifs depuis {@code RELANCE_INACTIVITE_JOURS} jours
     * (Lot 1 — appelé par le scheduler quotidien). Anti-doublon 7 j par étudiant.
     *
     * @return nombre de relances générées.
     */
    @Transactional
    public int relancerInactifs() {
        Instant seuil = Instant.now().minus(RELANCE_INACTIVITE_JOURS, ChronoUnit.DAYS);
        List<ProgressionEtudiant> inactifs = progressionRepository.findByDerniereActiviteBefore(seuil);
        int generes = 0;
        for (ProgressionEtudiant p : inactifs) {
            if (p.getUserId() == null || p.getSpaceId() == null || p.getDerniereActivite() == null) {
                continue;
            }
            boolean generee = genererRecommandation(p.getUserId(), p.getSpaceId(),
                    Recommandation.Type.RELANCE_INACTIVITE,
                    "Tu n'as pas révisé depuis plus de 7 jours. Une courte session (15 min) suffit pour garder le rythme.",
                    RELANCE_INACTIVITE_JOURS * 24);
            if (generee) {
                generes++;
            }
        }
        if (generes > 0) {
            log.info("RELANCE_INACTIVITE : {} relance(s) générée(s).", generes);
        }
        return generes;
    }

    /** Accusé de lecture d'une recommandation (PATCH /api/v1/recommandations/{id}/lue). */
    @Transactional
    public boolean marquerRecommandationLue(UUID userId, UUID recommandationId) {
        return recommandationRepository.findById(recommandationId)
                .filter(r -> r.getUserId().equals(userId))
                .map(r -> {
                    r.setLueLe(Instant.now());
                    recommandationRepository.save(r);
                    return true;
                })
                .orElse(false);
    }

    /** Purge les recommandations plus anciennes que {@code RECO_RETENTION_JOURS} jours. */
    @Transactional
    public void purgerAnciennesRecommandations() {
        recommandationRepository.deleteByGenereLeBefore(
                Instant.now().minus(RECO_RETENTION_JOURS, ChronoUnit.DAYS));
    }

    /**
     * Persiste une recommandation sauf doublon récent (même user/space/type/hash sous
     * {@code antiDoublonHeures} h). Le hash SHA-256 du contenu est stocké en colonne.
     *
     * @return true si une nouvelle recommandation a été créée.
     */
    private boolean genererRecommandation(UUID userId, UUID spaceId,
                                         Recommandation.Type type, String contenu,
                                         long antiDoublonHeures) {
        String hash = contenuHash(contenu);
        Instant depuis = Instant.now().minus(antiDoublonHeures, ChronoUnit.HOURS);
        boolean recent = recommandationRepository
                .findFirstByUserIdAndSpaceIdAndTypeAndContenuHashOrderByGenereLeDesc(
                        userId, spaceId, type, hash)
                .map(r -> r.getGenereLe() != null && r.getGenereLe().isAfter(depuis))
                .orElse(false);
        if (recent) {
            log.debug("Recommandation {} ignorée (doublon < {} h) userId={} spaceId={}.",
                    type, antiDoublonHeures, userId, spaceId);
            return false;
        }
        recommandationRepository.save(Recommandation.builder()
                .userId(userId).spaceId(spaceId)
                .type(type).contenu(contenu).contenuHash(hash)
                .build());
        return true;
    }

    /** Hash SHA-256 hex du contenu (64 car., cf. colonne contenu_hash). */
    static String contenuHash(String contenu) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((contenu == null ? "" : contenu).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(contenu == null ? 0 : contenu.hashCode());
        }
    }

    /**
     * Clé de notion persistée (Lot 1) : extraction lexicale (jusqu'à 2 mots significatifs)
     * avec repli sur la question normalisée tronquée quand l'extraction ne donne que
     * « général ». Évite que toutes les questions vagues partagent la même clé
     * {@code UNIQUE(space_id, notion)} et polluent les chapitres difficiles.
     */
    static String notionKey(String content) {
        String notion = extractNotion(content);
        if (!NOTION_GENERALE.equals(notion)) {
            return notion;
        }
        String normalisee = normaliseQuestion(content);
        if (normalisee.isBlank()) {
            return NOTION_GENERALE;
        }
        return normalisee.length() > NOTION_CLE_MAX_LENGTH
                ? normalisee.substring(0, NOTION_CLE_MAX_LENGTH)
                : normalisee;
    }

    /**
     * Extraction lexicale V1 (Lot 1) : jusqu'à 2 mots significatifs (longueur &gt; 4,
     * hors mots vides), joints par un espace, tronqués à 60 car. Repli « général ».
     * Un raffinement sémantique (embeddings / heading RAG) reste prévu au Lot 4.
     */
    static String extractNotion(String content) {
        if (content == null || content.isBlank()) {
            return NOTION_GENERALE;
        }
        String notion = Arrays.stream(content.toLowerCase(Locale.FRENCH).split("[^\\p{L}]+"))
                .filter(w -> w.length() > 4 && !STOP_WORDS.contains(w))
                .limit(2)
                .collect(Collectors.joining(" "));
        if (notion.isBlank()) {
            return NOTION_GENERALE;
        }
        return notion.length() > NOTION_CLE_MAX_LENGTH
                ? notion.substring(0, NOTION_CLE_MAX_LENGTH)
                : notion;
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
