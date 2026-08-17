package mg.esmia.miage.aicommon;

import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.exception.ApiException;
import mg.esmia.miage.common.exception.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Façade de bascule multi-provider LLM (contrat « Base de projet » : {@code groq} |
 * {@code gemini} | {@code ollama}).
 *
 * <p>Injecte la liste des providers comme {@code Supplier<ChatClient>} (résolus via
 * {@code ObjectProvider}) plutôt que comme {@code Map<String, ChatClient>} : un provider
 * absent (pas de clé API configurée) n'est pas un bean créé, et le resolver ne doit jamais
 * faire échouer le démarrage du service — juste rendre ce provider indisponible à l'appel.
 *
 * <p>{@code current()} lève une {@link ApiException} (code {@code LLM_PROVIDER_UNAVAILABLE},
 * HTTP 503) si le provider actif n'est pas disponible : la gestion de ce cas est déléguée
 * au circuit breaker du service appelant (fallback = message utilisateur clair), et ne doit
 * PAS retomber sur un repli silencieux vers un autre provider (changement de modèle non
 * explicite pour l'utilisateur).
 */
@Slf4j
public class ChatProviderResolver {

    private final Map<String, Supplier<ChatClient>> byProvider;
    private final String activeProvider;

    public ChatProviderResolver(Map<String, Supplier<ChatClient>> byProvider, String activeProvider) {
        this.byProvider = Map.copyOf(byProvider);
        this.activeProvider = activeProvider;
    }

    /**
     * @return le {@link ChatClient} du provider actif.
     * @throws ApiException si le provider configuré est inconnu ou indisponible.
     */
    public ChatClient current() {
        Supplier<ChatClient> provider = byProvider.get(activeProvider);
        if (provider == null) {
            throw new ApiException(ErrorCode.LLM_PROVIDER_UNAVAILABLE,
                    "Provider LLM inconnu : '" + activeProvider + "' (valeurs attendues : groq | gemini | ollama)", 503);
        }
        ChatClient client = provider.get();
        if (client == null) {
            log.warn("Provider LLM '{}' demandé mais non configuré (clé API manquante ?)", activeProvider);
            throw new ApiException(ErrorCode.LLM_PROVIDER_UNAVAILABLE,
                    "Provider LLM '" + activeProvider + "' non configuré dans cet environnement", 503);
        }
        return client;
    }

    /** Nom du provider actif (tel que configuré par {@code chat.llm.active-provider}). */
    public String activeProvider() {
        return activeProvider;
    }
}
