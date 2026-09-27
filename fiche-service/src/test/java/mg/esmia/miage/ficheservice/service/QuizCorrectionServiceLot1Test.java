package mg.esmia.miage.ficheservice.service;

import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.CreateCorrectionRequest;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.entity.QuizAttempt;
import mg.esmia.miage.ficheservice.entity.QuizCorrection;
import mg.esmia.miage.ficheservice.repository.QuizAttemptRepository;
import mg.esmia.miage.ficheservice.repository.QuizCorrectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Lot 1 : correction globale — sans score aucun event, avec score ventilation par auteur.
 */
@ExtendWith(MockitoExtension.class)
class QuizCorrectionServiceLot1Test {

    @Mock
    QuizCorrectionRepository correctionRepository;

    @Mock
    QuizAttemptRepository attemptRepository;

    @Mock
    QuizService quizService;

    @Mock
    RedisEventPublisher eventPublisher;

    @InjectMocks
    QuizCorrectionService correctionService;

    private Quiz quiz(UUID quizId) {
        return Quiz.builder()
                .spaceId(UUID.randomUUID()).userId(UUID.randomUUID())
                .title("Q").statut("PUBLIE").questionCount(10).build();
    }

    private QuizCorrection savedCorrection(UUID quizId) {
        return QuizCorrection.builder()
                .id(UUID.randomUUID()).quizId(quizId).attemptId(null)
                .enseignantId(UUID.randomUUID()).commentaire("ok").scoreCorrige(8).build();
    }

    @Test
    void correctionGlobaleSansScore_publieRien() {
        UUID quizId = UUID.randomUUID();
        when(quizService.findOrThrow(quizId)).thenReturn(quiz(quizId));
        when(correctionRepository.save(any())).thenAnswer(i -> savedCorrection(quizId));

        correctionService.addQuizCorrection(quizId, UUID.randomUUID(), true,
                new CreateCorrectionRequest("commentaire seul", null));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void correctionGlobaleAvecScore_ventileParAuteurDistinct() {
        UUID quizId = UUID.randomUUID();
        UUID auteur1 = UUID.randomUUID();
        UUID auteur2 = UUID.randomUUID();
        when(quizService.findOrThrow(quizId)).thenReturn(quiz(quizId));
        when(correctionRepository.save(any())).thenAnswer(i -> savedCorrection(quizId));
        when(attemptRepository.findByQuizIdOrderByAttemptedAtDesc(quizId)).thenReturn(List.of(
                QuizAttempt.builder().quizId(quizId).userId(auteur1).score(5).totalQuestions(10).build(),
                QuizAttempt.builder().quizId(quizId).userId(auteur1).score(6).totalQuestions(10).build(),
                QuizAttempt.builder().quizId(quizId).userId(auteur2).score(4).totalQuestions(10).build()));

        correctionService.addQuizCorrection(quizId, UUID.randomUUID(), true,
                new CreateCorrectionRequest("corrigé global", 8));

        // 2 auteurs distincts -> 2 events (publish-after-commit hors transaction = immédiat).
        verify(eventPublisher, times(2)).publish(anyString(), any());
    }

    @Test
    void correctionGlobaleAvecScoreSansTentative_publieRien() {
        UUID quizId = UUID.randomUUID();
        when(quizService.findOrThrow(quizId)).thenReturn(quiz(quizId));
        when(correctionRepository.save(any())).thenAnswer(i -> savedCorrection(quizId));
        when(attemptRepository.findByQuizIdOrderByAttemptedAtDesc(quizId)).thenReturn(List.of());

        correctionService.addQuizCorrection(quizId, UUID.randomUUID(), true,
                new CreateCorrectionRequest("corrigé", 9));

        verifyNoInteractions(eventPublisher);
    }
}
