package mg.esmia.miage.chatservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Petite façade sur les {@link ChatClient} disponibles : évite d'injecter une
 * {@code Map<String, ChatClient>} partout et centralise la bascule selon
 * {@code chat.llm.active-provider} (contrat : groq | gemini | ollama), avec repli
 * si le provider configuré est absent (ex. "gemini" pas encore câblé en dur).
 */
@Slf4j
public class ChatProviderResolver {

    private final Map<String, ChatClient> byProvider;
    private final String activeProvider;

    public ChatProviderResolver(Map<String, ChatClient> byProvider, String activeProvider) {
        this.byProvider = Map.copyOf(byProvider);
        this.activeProvider = activeProvider;
    }

    /**
     * @return le ChatClient du provider actif, ou un repli non bloquant si celui-ci
     * n'est pas enregistré (cas d'usage : provider désactivé ou pas encore intégré).
     * @throws IllegalStateException si aucun ChatClient n'est enregistré du tout.
     */
    public ChatClient current() {
        ChatClient active = byProvider.get(activeProvider);
        if (active != null) {
            return active;
        }
        ChatClient fallback = byProvider.get("ollama");
        if (fallback != null) {
            log.warn("Provider LLM '{}' non enregistré, repli sur 'ollama'", activeProvider);
            return fallback;
        }
        if (byProvider.isEmpty()) {
            throw new IllegalStateException("Aucun ChatClient LLM enregistré (groq/ollama)");
        }
        String firstKey = byProvider.keySet().iterator().next();
        log.warn("Provider LLM '{}' non enregistré, repli sur '{}'", activeProvider, firstKey);
        return byProvider.get(firstKey);
    }

    /** Nom du provider actif (tel que configuré par ACTIVE_LLM_PROVIDER). */
    public String activeProvider() {
        return activeProvider;
    }
}
