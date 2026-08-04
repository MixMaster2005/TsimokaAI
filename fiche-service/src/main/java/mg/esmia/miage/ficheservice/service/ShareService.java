package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.exception.BadRequestException;
import mg.esmia.miage.ficheservice.dto.ShareFicheRequest;
import mg.esmia.miage.ficheservice.entity.PartageFiche;
import mg.esmia.miage.ficheservice.repository.PartageFicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final PartageFicheRepository partageFicheRepository;
    private final FicheService ficheService;

    @Transactional
    public PartageFiche share(UUID ficheId, UUID partagePar, ShareFicheRequest request) {
        var fiche = ficheService.findOrThrow(ficheId);
        ficheService.assertOwnerOrAdmin(fiche, partagePar, false);

        if (request.groupeId() == null && request.destinataireId() == null) {
            throw new BadRequestException("Il faut fournir groupeId OU destinataireId");
        }
        PartageFiche partage = PartageFiche.builder()
                .ficheId(ficheId)
                .groupeId(request.groupeId())
                .destinataireId(request.destinataireId())
                .partagePar(partagePar)
                .build();
        return partageFicheRepository.save(partage);
    }

    public java.util.List<PartageFiche> listShares(UUID ficheId) {
        return partageFicheRepository.findByFicheId(ficheId);
    }
}
