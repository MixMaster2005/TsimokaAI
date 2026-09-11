package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.ficheservice.dto.GenerateQuizRequest;
import mg.esmia.miage.ficheservice.dto.QuizDetailResponse;
import mg.esmia.miage.ficheservice.dto.QuizPublicResponse;
import mg.esmia.miage.ficheservice.dto.QuizView;
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
    public QuizView generate(UUID userId, GenerateQuizRequest request, boolean fullAccess) {
        List<UUID> documentIds = request.documentIds() == null ? List.of() : request.documentIds();
        String scope = request.scope() == null ? "DOCUMENT" : request.scope();
        String difficulty = request.difficulty() == null ? "MOYEN" : request.difficulty();
        int questionCount = request.questionCount() == null ? 10 : request.questionCount();
        String statut = normalizeStatut(request.statut());

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
                .statut(statut)
                .build();

        String contentJson = generationService.generateQuizContent(quiz);
        quiz.setContentJson(contentJson);
        quiz.setGeneratedAt(Instant.now());
        quiz = quizRepository.save(quiz);

        return toView(quiz, fullAccess);
    }

    /** Surcharge historique : le créateur (owner) a un accès complet. */
    @Transactional
    public QuizView generate(UUID userId, GenerateQuizRequest request) {
        return generate(userId, request, true);
    }

    public QuizView getById(UUID id, UUID requesterId, boolean isAdmin, boolean fullAccess) {
        Quiz quiz = findOrThrow(id);
        assertOwnerOrAdmin(quiz, requesterId, isAdmin);
        boolean effectiveFull = fullAccess || isAdmin || quiz.getUserId().equals(requesterId);
        return toView(quiz, effectiveFull);
    }

    /** Surcharge historique. */
    public QuizView getById(UUID id, UUID requesterId, boolean isAdmin) {
        return getById(id, requesterId, isAdmin, isAdmin);
    }

    public Quiz findOrThrow(UUID id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz introuvable : " + id));
    }

    public List<QuizView> listMine(UUID spaceId, UUID userId, boolean fullAccess) {
        return quizRepository.findBySpaceIdAndUserId(spaceId, userId).stream()
                .map(q -> toView(q, fullAccess)).toList();
    }

    /** Surcharge historique : l'owner voit le détail complet. */
    public List<QuizView> listMine(UUID spaceId, UUID userId) {
        return listMine(spaceId, userId, true);
    }

    public List<QuizView> listAllMine(UUID userId, boolean fullAccess) {
        return quizRepository.findByUserIdOrderByGeneratedAtDesc(userId).stream()
                .map(q -> toView(q, fullAccess)).toList();
    }

    /** Surcharge historique : l'owner voit le détail complet. */
    public List<QuizView> listAllMine(UUID userId) {
        return listAllMine(userId, true);
    }

    public List<QuizView> listForSpace(UUID spaceId, boolean isAdmin, boolean fullAccess) {
        if (!isAdmin) {
            throw new ForbiddenException("Réservé aux enseignants");
        }
        return quizRepository.findBySpaceId(spaceId).stream()
                .map(q -> toView(q, fullAccess)).toList();
    }

    /** Surcharge historique. */
    public List<QuizView> listForSpace(UUID spaceId, boolean isAdmin) {
        return listForSpace(spaceId, isAdmin, true);
    }

    public List<QuizView> listForSpaceByStatut(UUID spaceId, String statut, boolean isAdmin, boolean fullAccess) {
        if (!isAdmin) {
            throw new ForbiddenException("Réservé aux enseignants");
        }
        return quizRepository.findBySpaceIdAndStatut(spaceId, statut).stream()
                .map(q -> toView(q, fullAccess)).toList();
    }

    @Transactional
    public QuizView publish(UUID id, UUID requesterId, boolean isAdmin) {
        Quiz quiz = findOrThrow(id);
        assertOwnerOrAdmin(quiz, requesterId, isAdmin);
        if ("PUBLIE".equalsIgnoreCase(quiz.getStatut())) {
            return QuizDetailResponse.from(quiz);
        }
        if (!"BROUILLON".equalsIgnoreCase(quiz.getStatut())) {
            throw new BadRequestException("Statut inattendu : " + quiz.getStatut());
        }
        quiz.setStatut("PUBLIE");
        quiz = quizRepository.save(quiz);
        return QuizDetailResponse.from(quiz);
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

    public void assertOwnerOrAdmin(Quiz quiz, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && !quiz.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à ce quiz");
        }
    }

    private QuizView toView(Quiz quiz, boolean fullAccess) {
        return fullAccess ? QuizDetailResponse.from(quiz) : QuizPublicResponse.from(quiz);
    }

    private String normalizeStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return "PUBLIE";
        }
        String upper = statut.trim().toUpperCase();
        if (!"BROUILLON".equals(upper) && !"PUBLIE".equals(upper)) {
            throw new BadRequestException("Statut invalide : " + statut + " (attendu BROUILLON ou PUBLIE)");
        }
        return upper;
    }
}
