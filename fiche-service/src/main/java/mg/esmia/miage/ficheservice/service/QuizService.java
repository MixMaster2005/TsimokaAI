package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.ficheservice.dto.GenerateQuizRequest;
import mg.esmia.miage.ficheservice.dto.QuizResponse;
import mg.esmia.miage.ficheservice.entity.Quiz;
import mg.esmia.miage.ficheservice.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizGenerationService generationService;

    @Transactional
    public QuizResponse generate(UUID userId, GenerateQuizRequest request) {
        List<UUID> documentIds = request.documentIds() == null ? List.of() : request.documentIds();
        String scope = request.scope() == null ? "DOCUMENT" : request.scope();
        String difficulty = request.difficulty() == null ? "MOYEN" : request.difficulty();
        int questionCount = request.questionCount() == null ? 10 : request.questionCount();

        Quiz quiz = Quiz.builder()
                .spaceId(request.spaceId())
                .userId(userId)
                .title(request.title() == null || request.title().isBlank() ? "Quiz de révision" : request.title())
                .scope(scope)
                .targetDocumentId(request.targetDocumentId())
                .targetTopic(request.targetTopic())
                .sourceFicheId(request.sourceFicheId())
                .sourceDocumentIds(documentIds.toArray(new UUID[0]))
                .difficulty(difficulty)
                .questionCount(questionCount)
                .build();

        String contentJson = generationService.generateQuizContent(quiz);
        quiz.setContentJson(contentJson);
        quiz.setGeneratedAt(Instant.now());
        quiz = quizRepository.save(quiz);

        return QuizResponse.from(quiz);
    }

    public QuizResponse getById(UUID id, UUID requesterId, boolean isAdmin) {
        Quiz quiz = findOrThrow(id);
        assertOwnerOrAdmin(quiz, requesterId, isAdmin);
        return QuizResponse.from(quiz);
    }

    public Quiz findOrThrow(UUID id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz introuvable : " + id));
    }

    public List<QuizResponse> listMine(UUID spaceId, UUID userId) {
        return quizRepository.findBySpaceIdAndUserId(spaceId, userId).stream()
                .map(QuizResponse::from).toList();
    }

    public List<QuizResponse> listAllMine(UUID userId) {
        return quizRepository.findByUserIdOrderByGeneratedAtDesc(userId).stream()
                .map(QuizResponse::from).toList();
    }

    public List<QuizResponse> listForSpace(UUID spaceId, boolean isAdmin) {
        if (!isAdmin) {
            throw new ForbiddenException("Réservé aux enseignants");
        }
        return quizRepository.findBySpaceId(spaceId).stream()
                .map(QuizResponse::from).toList();
    }

    @Transactional
    public void delete(UUID id, UUID requesterId, boolean isAdmin) {
        Quiz quiz = findOrThrow(id);
        assertOwnerOrAdmin(quiz, requesterId, isAdmin);
        quizRepository.delete(quiz);
    }

    @Transactional
    public void markObsoleteForSpace(UUID spaceId) {
        quizRepository.findBySpaceId(spaceId).forEach(q -> {
            q.setObsolete(true);
            quizRepository.save(q);
        });
    }

    @Transactional
    public void deleteAllForSpace(UUID spaceId) {
        quizRepository.deleteBySpaceId(spaceId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        quizRepository.deleteByUserId(userId);
    }

    void assertOwnerOrAdmin(Quiz quiz, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && !quiz.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à ce quiz");
        }
    }
}
