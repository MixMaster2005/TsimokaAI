package mg.esmia.miage.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("user-service API")
                .version("v1")
                .description("Authentification, comptes, JWT — TsimokaAI"));
    }
}
