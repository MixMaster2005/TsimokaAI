package mg.esmia.miage.ingestionservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre thread-safe des {@link SseEmitter} actifs, indexés par spaceId.
 * Chaque connexion SSE enregistre un emitter ; quand un document change de statut,
 * on notifie tous les emitters du space concerné.
 */
@Component
@Slf4j
public class SseEmittersRegistry {

    /** Timeout des connexions SSE : 30 minutes. */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    /** Map spaceId → Set of emitters. */
    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Enregistre un emitter pour un espace donné. L'auto-nettoyage est
     * configuré sur timeout / completion / erreur.
     */
    public void register(UUID spaceId, SseEmitter emitter) {
        emitters.computeIfAbsent(spaceId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE completed pour space {}", spaceId);
            remove(spaceId, emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE timeout pour space {}", spaceId);
            remove(spaceId, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE error pour space {}: {}", spaceId, e.getMessage());
            remove(spaceId, emitter);
        });

        log.debug("SSE emitter enregistré pour space {} (total: {})",
                spaceId, emitters.getOrDefault(spaceId, Set.of()).size());
    }

    /**
     * Envoie un événement {@code document_status} à tous les emitters d'un espace.
     *
     * @param spaceId  l'espace cible
     * @param payload  objet sérialisable en JSON (SseEmitter s'en charge)
     */
    public void notify(UUID spaceId, Object payload) {
        Set<SseEmitter> spaceEmitters = emitters.get(spaceId);
        if (spaceEmitters == null || spaceEmitters.isEmpty()) {
            return;
        }

        // Copie défensive pour éviter ConcurrentModificationException
        Set<SseEmitter> snapshot = Set.copyOf(spaceEmitters);
        for (SseEmitter emitter : snapshot) {
            try {
                emitter.send(SseEmitter.event()
                        .name("document_status")
                        .data(payload));
            } catch (IOException e) {
                log.warn("Échec envoi SSE, retrait de l'emitter: {}", e.getMessage());
                remove(spaceId, emitter);
            }
        }
    }

    private void remove(UUID spaceId, SseEmitter emitter) {
        Set<SseEmitter> spaceEmitters = emitters.get(spaceId);
        if (spaceEmitters != null) {
            spaceEmitters.remove(emitter);
            if (spaceEmitters.isEmpty()) {
                emitters.remove(spaceId);
            }
        }
    }
}
