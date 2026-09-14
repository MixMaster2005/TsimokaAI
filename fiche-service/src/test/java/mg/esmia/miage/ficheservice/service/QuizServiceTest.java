package mg.esmia.miage.ficheservice.service;

import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.ficheservice.dto.GenerateQuizRequest;
import mg.esmia.miage.ficheservice.dto.QuizDetailResponse;
import mg.esmia.miage.ficheservice.dto.QuizView;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.repository.QuizRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M2 : défaut BROUILLON, publish réservé enseignant.
 */
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    QuizRepository quizRepository;

    @Mock
    QuizGenerationService generationService;

    @InjectMocks
    QuizService quizService;

    @Test
    void generateSansStatut_donneBrouillon() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        GenerateQuizRequest req = new GenerateQuizRequest(
                spaceId, "Titre", null, null, null, null, null, null, null, null);

        when(generationService.generateQuizContent(any(Quiz.class))).thenReturn("{\"questions\":[]}");
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizView view = quizService.generate(userId, req, true);

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertEquals("BROUILLON", captor.getValue().getStatut());
        assertInstanceOf(QuizDetailResponse.class, view);
        assertEquals("BROUILLON", ((QuizDetailResponse) view).statut());
    }

    @Test
    void generateStatutInvalide_throwBadRequest() {
        UUID userId = UUID.randomUUID();
        GenerateQuizRequest req = new GenerateQuizRequest(
                UUID.randomUUID(), "Titre", null, null, null, null, null, null, null, "ARCHIVE");

        assertThrows(BadRequestException.class, () -> quizService.generate(userId, req, true));
        verifyNoInteractions(quizRepository);
    }

    @Test
    void publishEtudiantNonAdmin_throwForbidden() {
        UUID quizId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Quiz quiz = Quiz.builder()
                .spaceId(UUID.randomUUID()).userId(ownerId)
                .title("Q").statut("BROUILLON").build();
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        assertThrows(ForbiddenException.class, () -> quizService.publish(quizId, studentId, false));
        verify(quizRepository, never()).save(any());
    }

    @Test
    void publishAdmin_ok() {
        UUID quizId = UUID.randomUUID();
        Quiz quiz = Quiz.builder()
                .spaceId(UUID.randomUUID()).userId(UUID.randomUUID())
                .title("Q").statut("BROUILLON").build();
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizView view = quizService.publish(quizId, UUID.randomUUID(), true);

        assertEquals("PUBLIE", ((QuizDetailResponse) view).statut());
        verify(quizRepository).save(quiz);
        assertEquals("PUBLIE", quiz.getStatut());
    }
}
