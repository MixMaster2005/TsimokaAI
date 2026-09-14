package mg.esmia.miage.ficheservice.service;

import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import mg.esmia.miage.ficheservice.dto.SubmitQuizAttemptRequest;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.repository.QuizAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M2 : submit sur quiz BROUILLON -> 400 BadRequestException.
 */
@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    QuizAttemptRepository attemptRepository;

    @Mock
    QuizService quizService;

    @Mock
    QuizGenerationService generationService;

    @Mock
    RedisEventPublisher eventPublisher;

    @InjectMocks
    QuizAttemptService attemptService;

    @Test
    void submitBrouillon_throwBadRequest() {
        UUID quizId = UUID.randomUUID();
        Quiz quiz = Quiz.builder()
                .spaceId(UUID.randomUUID()).userId(UUID.randomUUID())
                .title("Q").statut("BROUILLON").questionCount(10).build();
        when(quizService.findOrThrow(quizId)).thenReturn(quiz);

        SubmitQuizAttemptRequest req = new SubmitQuizAttemptRequest("[]");

        assertThrows(BadRequestException.class,
                () -> attemptService.submit(quizId, UUID.randomUUID(), req));
        verify(attemptRepository, never()).save(any());
        verifyNoInteractions(generationService, eventPublisher);
    }
}
