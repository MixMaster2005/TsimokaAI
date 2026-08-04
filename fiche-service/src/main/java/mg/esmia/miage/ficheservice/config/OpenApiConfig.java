package mg.esmia.miage.ficheservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI ficheServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("fiche-service API")
                .version("v1")
                .description("Génération, partage, annotation et validation des fiches de révision"));
    }
}
