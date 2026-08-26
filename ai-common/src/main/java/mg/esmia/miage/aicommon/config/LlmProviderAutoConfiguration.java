package mg.esmia.miage.aicommon.config;

import mg.esmia.miage.aicommon.ChatProviderResolver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Auto-configuration de la bascule multi-provider LLM (noyau partagé {@code ai-common}).
 *
 * <p>Expose un {@link ChatProviderResolver} qui centralise le choix au runtime selon
 * {@code chat.llm.active-provider}. Chargeable dans tout service dépendant de
 * {@code ai-common} (chat-service, space-service, fiche-service).
 *
 * <p><b>Clients construits paresseusement, dans les suppliers</b> : une clé API absente
 * ou vide n'est PAS une erreur de démarrage — le provider est simplement indisponible
 * ({@code current()} renvoie un 503 LLM_PROVIDER_UNAVAILABLE, géré par le circuit
 * breaker du service appelant). Conséquence : {@code OpenAiChatAutoConfiguration} du
 * starter doit être EXCLUE dans les services (elle exige une clé non vide dès le boot
 * via son bean openAiApi). Les namespaces de configuration restent identiques
 * ({@code spring.ai.openai.*} pour Groq, {@code spring.ai.gemini.*} pour Gemini) ;
 * seule la création des beans change.
 *
 * <p><b>Ollama</b> reste auto-configuré par son starter ({@code OllamaChatModel} bean),
 * injecté ici via {@code ObjectProvider}.
 *
 * <p><b>Gemini</b> : endpoint compatible OpenAI officiel de Google, donc simple changement
 * de base-url. Le {@code completionsPath} diffère toutefois : {@code /v1/chat/completions}
 * pour Groq (la base finit par {@code /openai}), {@code /chat/completions} sans {@code /v1}
 * pour Gemini — d'où le chemin passé explicitement à la fabrique.
 */
@AutoConfiguration
@ConditionalOnClass(OllamaChatModel.class)
@AutoConfigureAfter({OllamaChatAutoConfiguration.class})
public class LlmProviderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ChatProviderResolver chatProviderResolver(
            // Namespace Groq = API compatible OpenAI exposée par Groq (cf. application.yml)
            @Value("${spring.ai.openai.base-url:https://api.groq.com/openai}") String groqBaseUrl,
            @Value("${spring.ai.openai.api-key:}") String groqApiKey,
            @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}") String groqModel,
            // Namespace Gemini = endpoint compatible OpenAI officiel de Google
            @Value("${spring.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/openai}") String geminiBaseUrl,
            @Value("${spring.ai.gemini.api-key:}") String geminiApiKey,
            @Value("${spring.ai.gemini.chat.options.model:gemini-2.5-flash}") String geminiModel,
            ObjectProvider<OllamaChatModel> ollamaModel,
            @Value("${chat.llm.active-provider:ollama}") String activeProvider) {
        return new ChatProviderResolver(Map.of(
                "groq", () -> openAiCompatibleClient(groqBaseUrl, "/v1/chat/completions", groqApiKey, groqModel),
                "gemini", () -> openAiCompatibleClient(geminiBaseUrl, "/chat/completions", geminiApiKey, geminiModel),
                "ollama", () -> {
                    OllamaChatModel model = ollamaModel.getIfAvailable();
                    return model == null ? null : ChatClient.create(model);
                }
        ), activeProvider);
    }

    /**
     * Construit le client d'un provider « compatible OpenAI » (Groq, Gemini).
     *
     * @return {@code null} si la clé est absente/vide — le resolver traite ce cas comme
     *         provider non configuré (jamais d'échec au démarrage).
     */
    private static ChatClient openAiCompatibleClient(String baseUrl, String completionsPath,
                                                     String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .completionsPath(completionsPath)
                .apiKey(apiKey)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
        return ChatClient.create(chatModel);
    }
}
