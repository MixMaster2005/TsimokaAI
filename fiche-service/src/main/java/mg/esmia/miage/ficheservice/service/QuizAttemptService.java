package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.QuizAttemptResponse;
import mg.esmia.miage.ficheservice.dto.SubmitQuizAttemptRequest;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.entity.QuizAttempt;
import mg.esmia.miage.common.events.QuizEvent;
import mg.esmia.miage.ficheservice.repository.QuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository attemptRepository;
    private final QuizService quizService;
    private final QuizGenerationService generationService;
    private final RedisEventPublisher eventPublisher;

    @Transactional
    public QuizAttemptResponse submit(UUID quizId, UUID userId, SubmitQuizAttemptRequest request) {
        Quiz quiz = quizService.findOrThrow(quizId);
        if (!"PUBLIE".equalsIgnoreCase(quiz.getStatut())) {
            throw new BadRequestException("Quiz non publié");
        }

        // Score la tentative
        String scoredAnswers = generationService.scoreAttempt(quiz.getContentJson(), request.answersJson());

        // Compter le nombre de bonnes réponses
        int score = countCorrectAnswers(scoredAnswers);

        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(quizId)
                .userId(userId)
                .answersJson(scoredAnswers)
                .score(score)
                .totalQuestions(quiz.getQuestionCount())
                .build();

        attempt = attemptRepository.save(attempt);

        // Consommé par analytics-service (progression) et gamification-service (badges).
        // Publish-after-commit : différé après le commit pour éviter un événement
        // fantôme en cas de rollback ; envoi immédiat hors transaction (tests).
        QuizEvent event = QuizEvent.submitted(quizId.toString(), quiz.getSpaceId().toString(),
                userId.toString(), attempt.getScore(), attempt.getTotalQuestions());
        publishAfterCommit(EventChannels.FICHE_EVENTS, event);

        return QuizAttemptResponse.from(attempt);
    }

    public List<QuizAttemptResponse> listMyAttempts(UUID quizId, UUID userId) {
        return attemptRepository.findByQuizIdAndUserIdOrderByAttemptedAtDesc(quizId, userId).stream()
                .map(QuizAttemptResponse::from).toList();
    }

    public List<QuizAttemptResponse> listAllAttempts(UUID quizId, UUID requesterId, boolean isAdmin) {
        Quiz quiz = quizService.findOrThrow(quizId);
        quizService.assertOwnerOrAdmin(quiz, requesterId, isAdmin);
        return attemptRepository.findByQuizIdOrderByAttemptedAtDesc(quizId).stream()
                .map(QuizAttemptResponse::from).toList();
    }

    /** Surcharge historique non sécurisée — conservée pour compatibilité, à éviter. */
    @Deprecated
    public List<QuizAttemptResponse> listAllAttempts(UUID quizId) {
        return attemptRepository.findByQuizIdOrderByAttemptedAtDesc(quizId).stream()
                .map(QuizAttemptResponse::from).toList();
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

    private int countCorrectAnswers(String scoredAnswersJson) {
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(scoredAnswersJson);
            int count = 0;
            for (var answer : node) {
                if (answer.get("correct").asBoolean()) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
