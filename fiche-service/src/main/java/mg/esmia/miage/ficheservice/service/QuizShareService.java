package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.ficheservice.dto.QuizShareResponse;
import mg.esmia.miage.ficheservice.dto.ShareQuizRequest;
import mg.esmia.miage.ficheservice.entity.QuizShare;
import mg.esmia.miage.ficheservice.repository.QuizShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizShareService {

    private final QuizShareRepository quizShareRepository;
    private final QuizService quizService;

    @Transactional
    public QuizShareResponse share(UUID quizId, UUID partagePar, ShareQuizRequest request) {
        var quiz = quizService.findOrThrow(quizId);
        quizService.assertOwnerOrAdmin(quiz, partagePar, false);

        if (request.groupeId() == null && request.destinataireId() == null) {
            throw new BadRequestException("Il faut fournir groupeId OU destinataireId");
        }

        QuizShare share = QuizShare.builder()
                .quizId(quizId)
                .groupeId(request.groupeId())
                .destinataireId(request.destinataireId())
                .partagePar(partagePar)
                .build();

        return QuizShareResponse.from(quizShareRepository.save(share));
    }

    public List<QuizShareResponse> listShares(UUID quizId) {
        return quizShareRepository.findByQuizId(quizId).stream()
                .map(QuizShareResponse::from).toList();
    }
}
