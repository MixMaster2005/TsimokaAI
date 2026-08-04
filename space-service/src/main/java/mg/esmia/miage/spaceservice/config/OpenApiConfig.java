package mg.esmia.miage.spaceservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI spaceServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("space-service API")
                .version("v1")
                .description("CRUD espaces de cours, groupes de travail, persona pédagogique"));
    }
}
