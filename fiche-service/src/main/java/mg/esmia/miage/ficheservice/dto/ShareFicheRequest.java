package mg.esmia.miage.ficheservice.dto;

import java.util.UUID;

/** Fournir exactement un des deux : groupeId (partage à un groupe) ou destinataireId (partage direct). */
public record ShareFicheRequest(UUID groupeId, UUID destinataireId) {
}
