package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.ficheservice.dto.QuizAttemptResponse;
import mg.esmia.miage.ficheservice.dto.SubmitQuizAttemptRequest;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.entity.QuizAttempt;
import mg.esmia.miage.ficheservice.repository.QuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository attemptRepository;
    private final QuizService quizService;
    private final QuizGenerationService generationService;

    @Transactional
    public QuizAttemptResponse submit(UUID quizId, UUID userId, SubmitQuizAttemptRequest request) {
        Quiz quiz = quizService.findOrThrow(quizId);

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
        return QuizAttemptResponse.from(attempt);
    }

    public List<QuizAttemptResponse> listMyAttempts(UUID quizId, UUID userId) {
        return attemptRepository.findByQuizIdAndUserIdOrderByAttemptedAtDesc(quizId, userId).stream()
                .map(QuizAttemptResponse::from).toList();
    }

    public List<QuizAttemptResponse> listAllAttempts(UUID quizId) {
        return attemptRepository.findByQuizIdOrderByAttemptedAtDesc(quizId).stream()
                .map(QuizAttemptResponse::from).toList();
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
