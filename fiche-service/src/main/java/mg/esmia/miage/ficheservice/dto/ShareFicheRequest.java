package mg.esmia.miage.ficheservice.dto;

import java.util.UUID;

/**
 * Requête de partage de fiche. Fourrir exactement un des deux :
 * groupeId (partage à un groupe) OU destinataireId (partage direct).
 */
@OneOfTwo
public record ShareFicheRequest(UUID groupeId, UUID destinataireId) {
}
