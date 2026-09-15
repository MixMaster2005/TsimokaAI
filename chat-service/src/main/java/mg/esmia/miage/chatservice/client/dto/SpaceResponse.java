package mg.esmia.miage.chatservice.client.dto;

import java.util.UUID;

/**
 * Vue minimale de l'espace de cours renvoyée par space-service (le contrat d'enveloppe
 * {@code ApiResponse<SpaceResponse>} est partagé via le module common ; les champs non
 * utilisés sont ignorés par Jackson).
 *
 * <p>{@code personaVersion} expose la version courante du persona (cf. space-service
 * {@code SpaceResponse.personaVersion}) pour la traçabilité exacte persona par message.
 * Nullable : absent (ancien space-service) ou espace sans version → null.
 */
public record SpaceResponse(UUID id, String name, String assistantPersona, Integer personaVersion) {
}
