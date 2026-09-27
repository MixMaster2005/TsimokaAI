package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.CreateCorrectionRequest;
import mg.esmia.miage.ficheservice.dto.QuizCorrectionResponse;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.entity.QuizAttempt;
import mg.esmia.miage.ficheservice.entity.QuizCorrection;
import mg.esmia.miage.common.events.QuizEvent;
import mg.esmia.miage.ficheservice.repository.QuizAttemptRepository;
import mg.esmia.miage.ficheservice.repository.QuizCorrectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizCorrectionService {

    private final QuizCorrectionRepository correctionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizService quizService;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public QuizCorrectionResponse addQuizCorrection(UUID quizId, UUID enseignantId, boolean isAdmin, CreateCorrectionRequest req) {
        Quiz quiz = quizService.findOrThrow(quizId);
        quizService.assertOwnerOrAdmin(quiz, enseignantId, isAdmin);

        QuizCorrection correction = correctionRepository.save(QuizCorrection.builder()
                .quizId(quizId)
                .attemptId(null)
                .enseignantId(enseignantId)
                .commentaire(req == null ? null : req.commentaire())
                .scoreCorrige(req == null ? null : req.scoreCorrige())
                .build());

        // Lot 1 : une correction globale SANS score n'a aucun impact métrique — ne pas
        // publier de QUIZ_CORRECTED vide (userId/score nulls) que les consommateurs
        // ignorent avec un warn. Une correction globale AVEC score est ventilée à
        // chaque étudiant ayant tenté le quiz (un event par userIdEtu).
        Integer scoreCorrige = req == null ? null : req.scoreCorrige();
        if (scoreCorrige == null) {
            log.info("Correction globale sans score sur quiz {} : aucun event QUIZ_CORRECTED publié.", quizId);
            return QuizCorrectionResponse.from(correction);
        }
        List<UUID> auteurs = attemptRepository.findByQuizIdOrderByAttemptedAtDesc(quizId).stream()
                .map(QuizAttempt::getUserId)
                .filter(u -> u != null)
                .distinct()
                .toList();
        if (auteurs.isEmpty()) {
            log.warn("Correction globale avec score sur quiz {} sans tentative : aucun event publié.", quizId);
            return QuizCorrectionResponse.from(correction);
        }
        for (UUID auteur : auteurs) {
            publishAfterCommit(EventChannels.FICHE_EVENTS,
                    QuizEvent.corrected(quizId.toString(), null, auteur.toString(),
                            quiz.getSpaceId().toString(), enseignantId.toString(),
                            scoreCorrige, quiz.getQuestionCount()));
        }
        log.info("Correction globale avec score sur quiz {} : {} event(s) QUIZ_CORRECTED ventilé(s).",
                quizId, auteurs.size());

        return QuizCorrectionResponse.from(correction);
    }

    @Transactional
    public QuizCorrectionResponse addAttemptCorrection(UUID quizId, UUID attemptId, UUID enseignantId, boolean isAdmin, CreateCorrectionRequest req) {
        Quiz quiz = quizService.findOrThrow(quizId);
        quizService.assertOwnerOrAdmin(quiz, enseignantId, isAdmin);

        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Tentative introuvable : " + attemptId));
        if (!attempt.getQuizId().equals(quizId)) {
            throw new ForbiddenException("La tentative n'appartient pas à ce quiz");
        }

        QuizCorrection correction = correctionRepository.save(QuizCorrection.builder()
                .quizId(quizId)
                .attemptId(attemptId)
                .enseignantId(enseignantId)
                .commentaire(req == null ? null : req.commentaire())
                .scoreCorrige(req == null ? null : req.scoreCorrige())
                .build());

        // Score effectif : corrigé s'il est fourni, sinon score auto de la tentative
        // (une correction par commentaire seul crédite quand même l'avancement).
        // Idempotence côté consommateurs (clé attemptId, UNIQUE) : une redélivrance
        // de QUIZ_CORRECTED rejoue les scores sans incrémenter nbQuizPasses.
        Integer scoreEffectif = req != null && req.scoreCorrige() != null ? req.scoreCorrige() : attempt.getScore();
        publishAfterCommit(EventChannels.FICHE_EVENTS,
                QuizEvent.corrected(quizId.toString(), attemptId.toString(), attempt.getUserId().toString(),
                        quiz.getSpaceId().toString(), enseignantId.toString(),
                        scoreEffectif, attempt.getTotalQuestions()));

        return QuizCorrectionResponse.from(correction);
    }

    public List<QuizCorrectionResponse> listByQuiz(UUID quizId) {
        quizService.findOrThrow(quizId);
        return correctionRepository.findByQuizId(quizId).stream()
                .map(QuizCorrectionResponse::from).toList();
    }

    public List<QuizCorrectionResponse> listByAttempt(UUID quizId, UUID attemptId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Tentative introuvable : " + attemptId));
        if (!attempt.getQuizId().equals(quizId)) {
            throw new ForbiddenException("La tentative n'appartient pas à ce quiz");
        }
        return correctionRepository.findByAttemptId(attemptId).stream()
                .map(QuizCorrectionResponse::from).toList();
    }

    private void publishAfterCommit(String channel, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publish(channel, event);
                }
            });
        } else {
            eventPublisher.publish(channel, event);
        }
    }
}
