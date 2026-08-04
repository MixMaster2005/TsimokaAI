package mg.esmia.miage.gamificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.exception.ForbiddenException;
import mg.esmia.miage.common.exception.ResourceNotFoundException;
import mg.esmia.miage.gamificationservice.dto.CreateRappelRequest;
import mg.esmia.miage.gamificationservice.dto.RappelResponse;
import mg.esmia.miage.gamificationservice.entity.Rappel;
import mg.esmia.miage.gamificationservice.repository.RappelRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RappelService {

    private final RappelRepository rappelRepository;

    @Transactional
    public RappelResponse create(UUID userId, CreateRappelRequest request) {
        Rappel rappel = Rappel.builder()
                .userId(userId)
                .spaceId(request.spaceId())
                .message(request.message())
                .prevuLe(request.prevuLe())
                .build();
        return RappelResponse.from(rappelRepository.save(rappel));
    }

    public List<RappelResponse> listMine(UUID userId) {
        return rappelRepository.findByUserId(userId).stream().map(RappelResponse::from).toList();
    }

    @Transactional
    public void delete(UUID id, UUID requesterId) {
        Rappel rappel = rappelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rappel introuvable : " + id));
        if (!rappel.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Accès refusé à ce rappel");
        }
        rappelRepository.delete(rappel);
    }

    /**
     * Vérifie toutes les 5 minutes les rappels arrivés à échéance.
     * NB : la livraison effective (email/push) n'est PAS implémentée ici — il n'existe
     * pas de service de notification dans la plateforme actuelle. Ce job se contente de
     * marquer le rappel comme "envoyé" et de journaliser ; brancher un vrai canal de
     * livraison (SMTP, web push...) est une extension possible, non bloquante.
     */
    @Scheduled(fixedDelayString = "${gamification.rappels.check-interval-ms:300000}")
    @Transactional
    public void processDueReminders() {
        List<Rappel> dus = rappelRepository.findByEnvoyeFalseAndPrevuLeBefore(Instant.now());
        for (Rappel rappel : dus) {
            log.info("Rappel dû pour l'utilisateur {} : \"{}\" (livraison réelle non implémentée)",
                    rappel.getUserId(), rappel.getMessage());
            rappel.setEnvoye(true);
            rappelRepository.save(rappel);
        }
    }
}
