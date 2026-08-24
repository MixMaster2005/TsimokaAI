package mg.esmia.miage.spaceservice.dto;

/**
 * Code d'invitation d'un espace, réservé au propriétaire (endpoint dédié plutôt que
 * champ de {@link SpaceResponse} : le code ne doit PAS être exposé à un simple
 * membre ou visiteur — c'est ce qui protège l'accès à l'espace).
 */
public record InviteCodeResponse(String inviteCode) {
}
