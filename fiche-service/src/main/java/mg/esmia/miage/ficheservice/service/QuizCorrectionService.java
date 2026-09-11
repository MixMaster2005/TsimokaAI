package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.CreateCorrectionRequest;
import mg.esmia.miage.ficheservice.dto.QuizCorrectionResponse;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.entity.QuizAttempt;
import mg.esmia.miage.ficheservice.entity.QuizCorrection;
import mg.esmia.miage.ficheservice.messaging.QuizEvent;
import mg.esmia.miage.ficheservice.repository.QuizAttemptRepository;
import mg.esmia.miage.ficheservice.repository.QuizCorrectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

        eventPublisher.publish(EventChannels.FICHE_EVENTS,
                QuizEvent.corrected(quizId.toString(), null, null,
                        quiz.getSpaceId().toString(), enseignantId.toString(),
                        req == null ? null : req.scoreCorrige(), quiz.getQuestionCount()));

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
        Integer scoreEffectif = req != null && req.scoreCorrige() != null ? req.scoreCorrige() : attempt.getScore();
        eventPublisher.publish(EventChannels.FICHE_EVENTS,
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
}
