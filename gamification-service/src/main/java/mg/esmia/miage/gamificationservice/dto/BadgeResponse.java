package mg.esmia.miage.gamificationservice.dto;

import mg.esmia.miage.gamificationservice.entity.Badge;

import java.util.UUID;

public record BadgeResponse(UUID id, String code, String nom, String description, String icone, boolean obtenu) {
    public static BadgeResponse from(Badge b, boolean obtenu) {
        return new BadgeResponse(b.getId(), b.getCode(), b.getNom(), b.getDescription(), b.getIcone(), obtenu);
    }
}
