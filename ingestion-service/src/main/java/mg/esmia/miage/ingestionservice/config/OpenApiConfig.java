package mg.esmia.miage.ingestionservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI ingestionServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ingestion-service API")
                .version("v1")
                .description("Upload, extraction, chunking, embedding, indexation vectorielle"));
    }
}
