package mg.esmia.miage.chatservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import jakarta.annotation.PostConstruct;

/**
 * Bascule de provider LLM par configuration (contrat "Base de projet" : ACTIVE_LLM_PROVIDER
 * = groq | gemini | ollama). Spring AI instancie automatiquement un ChatClient par starter
 * présent sur le classpath (OpenAiChatModel pour Groq via base-url, OllamaChatModel pour le
 * fallback) ; il reste à choisir dynamiquement lequel utiliser dans ChatService selon cette
 * propriété — TODO, car cela dépend du choix définitif d'intégration Gemini (cf. pom.xml).
 *
 * Piste recommandée : injecter les deux (ou trois) ChatModel candidats ici, exposer un bean
 * "activeChatModel" sélectionné via @ConditionalOnProperty ou par une simple fabrique Java
 * lisant ACTIVE_LLM_PROVIDER au démarrage, et l'utiliser dans ChatService.
 */
@Configuration
@Slf4j
public class LlmProviderConfig {

    @Value("${chat.llm.active-provider:ollama}")
    private String activeProvider;

    @PostConstruct
    public void logActiveProvider() {
        log.info("ACTIVE_LLM_PROVIDER = {} (câblage effectif du ChatClient : TODO dans ChatService)", activeProvider);
    }

    // TODO: @Bean activeChatModel() une fois la stratégie de sélection tranchée.
}
