package mg.esmia.miage.chatservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Bascule de provider LLM par configuration (contrat "Base de projet" : ACTIVE_LLM_PROVIDER
 * = groq | gemini | ollama).
 *
 * <p><b>Groq</b> et <b>Ollama</b> : les starters Spring AI auto-configurent leur ChatModel
 * (OpenAiChatModel pointé sur le base-url Groq, OllamaChatModel pour le fallback local). On
 * expose un {@link ChatClient} par provider, qualifié par son identifiant, plus un
 * {@link ChatProviderResolver} qui centralise le choix au runtime.
 *
 * <p><b>Gemini</b> : Google expose une API compatible OpenAI officielle
 * ({@code https://generativelanguage.googleapis.com/v1beta/openai/}, clé AI Studio, noms de
 * modèles Gemini) — c'est un simple changement de base-url, comme pour Groq. MAIS le starter
 * OpenAI ne permet qu'UNE auto-configuration (déjà prise par Groq) : le bean auto-configuré
 * {@code openAiChatModel} est en effet {@code @ConditionalOnMissingBean(OpenAiApi.class)}.
 * On construit donc ici un modèle Gemini via une seconde instance {@link OpenAiApi}, en
 * <b>variable locale</b> de la méthode {@code @Bean} (jamais un bean Spring), pour ne pas
 * supprimer l'auto-config Groq. Namespace dédié : {@code spring.ai.gemini.*}.
 */
@Configuration
@Slf4j
public class LlmProviderConfig {

    @Value("${chat.llm.active-provider:ollama}")
    private String activeProvider;

    @Bean
    @Qualifier("groq")
    ChatClient groqChatClient(OpenAiChatModel groqModel) {
        return ChatClient.create(groqModel);
    }

    @Bean
    @Qualifier("ollama")
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
    ChatProviderResolver chatProviderResolver(
            @Qualifier("groq") ChatClient groq,
            @Qualifier("ollama") ChatClient ollama,
            @Qualifier("gemini") ChatClient gemini,
            @Value("${chat.llm.active-provider:ollama}") String activeProvider) {
        return new ChatProviderResolver(Map.of(
                "groq", groq,
                "gemini", gemini,
                "ollama", ollama
        ), activeProvider);
    }

    @PostConstruct
    public void logActiveProvider() {
        log.info("ACTIVE_LLM_PROVIDER = {} (bascule via ChatProviderResolver)", activeProvider);
    }
}
