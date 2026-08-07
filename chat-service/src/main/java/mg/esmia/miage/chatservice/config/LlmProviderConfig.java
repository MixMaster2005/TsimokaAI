package mg.esmia.miage.chatservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Bascule de provider LLM par configuration (contrat "Base de projet" : ACTIVE_LLM_PROVIDER
 * = groq | gemini | ollama).
 *
 * Chaque starter Spring AI auto-configure un ChatModel : OpenAiChatModel pointé sur le
 * base-url Groq (API compatible OpenAI) et OllamaChatModel pour le fallback local. On expose
 * ici un ChatClient par provider, qualifié par son identifiant, plus un
 * {@link ChatProviderResolver} qui centralise le choix au runtime (plus d'injection de
 * Map/String partout dans les services).
 *
 * Gemini reste un point ouvert (cf. ARCHITECTURE.md §6) : le starter OpenAI n'acceptant
 * qu'une seule auto-configuration, il faudrait un bean OpenAiApi/OpenAiChatModel manuel
 * pointé sur l'endpoint compatible Gemini (même mécanisme que docling-worker), puis
 * l'ajouter à la Map du résolveur.
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

    @Bean
    ChatProviderResolver chatProviderResolver(
            @Qualifier("groq") ChatClient groq,
            @Qualifier("ollama") ChatClient ollama,
            @Value("${chat.llm.active-provider:ollama}") String activeProvider) {
        return new ChatProviderResolver(Map.of(
                "groq", groq,
                "ollama", ollama
                // "gemini", geminiChatClient TODO: intégration Gemini (cf. ARCHITECTURE.md §6)
        ), activeProvider);
    }

    @PostConstruct
    public void logActiveProvider() {
        log.info("ACTIVE_LLM_PROVIDER = {} (bascule via ChatProviderResolver)", activeProvider);
    }
}
