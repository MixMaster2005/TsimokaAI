package mg.esmia.miage.gamificationservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.gamificationservice.dto.BadgeResponse;
import mg.esmia.miage.gamificationservice.repository.BadgeObtenuRepository;
import mg.esmia.miage.gamificationservice.repository.BadgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeQueryService {

    private final BadgeRepository badgeRepository;
    private final BadgeObtenuRepository badgeObtenuRepository;

    /** Catalogue complet, avec indicateur "obtenu" pour l'utilisateur courant. */
    public List<BadgeResponse> listForUser(UUID userId) {
        Set<UUID> badgesObtenus = badgeObtenuRepository.findByUserId(userId).stream()
                .map(bo -> bo.getBadgeId()).collect(Collectors.toSet());
        return badgeRepository.findAll().stream()
                .map(b -> BadgeResponse.from(b, badgesObtenus.contains(b.getId())))
                .toList();
    }
}
