package mg.esmia.miage.ingestionservice.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client Qdrant générique, prêt à l'emploi. La création des collections
 * (une par espace : "chunks_{space_id}", cf. contrat) et les opérations
 * d'upsert/recherche restent à implémenter dans IngestionPipelineService / chat-service.
 */
@Configuration
public class QdrantConfig {

    @Bean
    public QdrantClient qdrantClient(@Value("${qdrant.host}") String host,
                                      @Value("${qdrant.port}") int port,
                                      @Value("${qdrant.use-tls:false}") boolean useTls) {
        return new QdrantClient(QdrantGrpcClient.newBuilder(host, port, useTls).build());
    }
}
