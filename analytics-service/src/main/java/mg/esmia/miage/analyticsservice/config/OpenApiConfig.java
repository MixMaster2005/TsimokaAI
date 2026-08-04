package mg.esmia.miage.analyticsservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI analyticsServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("analytics-service API")
                .version("v1")
                .description("Tableaux de bord étudiant/enseignant, statistiques d'usage, recommandations"));
    }
}
