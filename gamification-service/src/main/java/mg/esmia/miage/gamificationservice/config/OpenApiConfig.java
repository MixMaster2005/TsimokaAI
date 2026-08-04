package mg.esmia.miage.gamificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI gamificationServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("gamification-service API")
                .version("v1")
                .description("Objectifs de révision, badges, suivi hebdomadaire, rappels"));
    }
}
