package mg.esmia.miage.ficheservice.dto;

import mg.esmia.miage.ficheservice.entity.PartageFiche;

import java.time.Instant;
import java.util.UUID;

public record PartageFicheResponse(
        UUID id,
        UUID ficheId,
        UUID groupeId,
        UUID destinataireId,
        UUID partagePar,
        Instant sharedAt
) {
    public static PartageFicheResponse from(PartageFiche p) {
        return new PartageFicheResponse(
                p.getId(), p.getFicheId(), p.getGroupeId(),
                p.getDestinataireId(), p.getPartagePar(), p.getSharedAt()
        );
    }
}
