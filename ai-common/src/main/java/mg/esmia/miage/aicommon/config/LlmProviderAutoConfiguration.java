package mg.esmia.miage.aicommon.config;

import mg.esmia.miage.aicommon.ChatProviderResolver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Auto-configuration de la bascule multi-provider LLM (noyau partagé {@code ai-common}).
 *
 * <p>Expose un {@link ChatClient} par provider (Groq, Gemini, Ollama) + un
 * {@link ChatProviderResolver} qui centralise le choix au runtime selon
 * {@code chat.llm.active-provider}. Chargeable dans tout service dépendant de
 * {@code ai-common} (chat-service, space-service, fiche-service).
 *
 * <p>Les providers sont injectés dans le resolver via {@code ObjectProvider<ChatClient>}
 * (pas d'injection directe) : un provider absent — pas de clé API configurée — ne fait
 * jamais échouer le démarrage du service, il est simplement indisponible dans le resolver.
 *
 * <p><b>Groq</b> et <b>Ollama</b> : les starters Spring AI auto-configurent leur
 * {@code ChatModel} (OpenAiChatModel pointé sur le base-url Groq, OllamaChatModel pour le
 * fallback local) ; on ne fait que les envelopper dans un {@code ChatClient} qualifié.
 *
 * <p><b>Gemini</b> : Google expose une API compatible OpenAI officielle
 * ({@code https://generativelanguage.googleapis.com/v1beta/openai/}, clé AI Studio, noms de
 * modèles Gemini) — c'est un simple changement de base-url, comme pour Groq. MAIS le starter
 * OpenAI ne permet qu'UNE auto-configuration (déjà prise par Groq) : le bean auto-configuré
 * {@code openAiChatModel} est en effet {@code @ConditionalOnMissingBean(OpenAiApi.class)}.
 * On construit donc un modèle Gemini via une seconde instance {@link OpenAiApi}, en
 * <b>variable locale</b> de la méthode {@code @Bean} (jamais un bean Spring), pour ne pas
 * supprimer l'auto-config Groq. Namespace dédié : {@code spring.ai.gemini.*}.
 */
@AutoConfiguration
@ConditionalOnClass(ChatModel.class)
@AutoConfigureAfter({OpenAiChatAutoConfiguration.class, OllamaChatAutoConfiguration.class})
public class LlmProviderAutoConfiguration {

    @Bean
    @Qualifier("groq")
    @ConditionalOnBean(OpenAiChatModel.class)
    ChatClient groqChatClient(OpenAiChatModel groqModel) {
        return ChatClient.create(groqModel);
    }

    @Bean
    @Qualifier("ollama")
    @ConditionalOnBean(OllamaChatModel.class)
    ChatClient ollamaChatClient(OllamaChatModel ollamaModel) {
        return ChatClient.create(ollamaModel);
    }

    /**
     * Gemini via son endpoint compatible OpenAI. Construit un {@link OpenAiApi} +
     * {@link OpenAiChatModel} locaux (non beans) pour ne pas écraser l'auto-config Groq.
     *
     * <p>NB : le {@code completionsPath} par défaut de Spring AI est {@code /v1/chat/completions}
     * (correct pour Groq, dont la base finit par {@code /openai}) ; l'endpoint Gemini, lui,
     * est {@code <base>/chat/completions} sans {@code /v1} — d'où le {@code /chat/completions}
     * explicite ici.
     */
    @Bean
    @Qualifier("gemini")
    ChatClient geminiChatClient(
            @Value("${spring.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/openai}") String geminiBaseUrl,
            @Value("${spring.ai.gemini.api-key:}") String geminiApiKey,
            @Value("${spring.ai.gemini.chat.options.model:gemini-2.5-flash}") String geminiModel) {
        OpenAiApi geminiApi = OpenAiApi.builder()
                .baseUrl(geminiBaseUrl)
                .completionsPath("/chat/completions")
                .apiKey(geminiApiKey)
                .build();
        OpenAiChatModel geminiChatModel = OpenAiChatModel.builder()
                .openAiApi(geminiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(geminiModel).build())
                .build();
        return ChatClient.create(geminiChatModel);
    }

    @Bean
    @ConditionalOnMissingBean
    ChatProviderResolver chatProviderResolver(
            @Qualifier("groq") ObjectProvider<ChatClient> groq,
            @Qualifier("ollama") ObjectProvider<ChatClient> ollama,
            @Qualifier("gemini") ObjectProvider<ChatClient> gemini,
            @Value("${chat.llm.active-provider:ollama}") String activeProvider) {
        return new ChatProviderResolver(Map.of(
                "groq", toSupplier(groq),
                "gemini", toSupplier(gemini),
                "ollama", toSupplier(ollama)
        ), activeProvider);
    }

    private static Supplier<ChatClient> toSupplier(ObjectProvider<ChatClient> provider) {
        return () -> provider.getIfAvailable();
    }
}
