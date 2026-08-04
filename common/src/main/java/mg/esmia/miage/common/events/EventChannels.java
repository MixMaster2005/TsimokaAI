package mg.esmia.miage.common.events;

/**
 * Noms des canaux Redis Pub/Sub (contrat d'événements - "Base de projet" / CDC §5.4).
 * Tout consommateur DOIT être idempotent : recevoir deux fois le même événement
 * ne doit pas produire d'effet de bord.
 */
public final class EventChannels {
    private EventChannels() {
    }

    public static final String INGESTION_EVENTS = "ingestion.events";
    public static final String CHAT_EVENTS = "chat.events";
    public static final String FICHE_EVENTS = "fiche.events";
    public static final String SPACE_EVENTS = "space.events";
    public static final String USER_EVENTS = "user.events";
}
