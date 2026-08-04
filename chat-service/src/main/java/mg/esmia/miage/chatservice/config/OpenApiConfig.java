package mg.esmia.miage.chatservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI chatServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("chat-service API")
                .version("v1")
                .description("Orchestration RAG + historique de conversation"));
    }
}
