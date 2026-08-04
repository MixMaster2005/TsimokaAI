package mg.esmia.miage.gamificationservice.dto;

import mg.esmia.miage.gamificationservice.entity.Rappel;

import java.time.Instant;
import java.util.UUID;

public record RappelResponse(UUID id, UUID spaceId, String message, Instant prevuLe, boolean envoye) {
    public static RappelResponse from(Rappel r) {
        return new RappelResponse(r.getId(), r.getSpaceId(), r.getMessage(), r.getPrevuLe(), r.isEnvoye());
    }
}
