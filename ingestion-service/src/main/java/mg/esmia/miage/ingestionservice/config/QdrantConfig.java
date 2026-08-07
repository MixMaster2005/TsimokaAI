package mg.esmia.miage.ingestionservice.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client Qdrant générique, prêt à l'emploi. Pattern multi-tenant (Option A) : **une seule
 * collection** "chunks" (cf. {@code qdrant.collection-name}), chaque point portant son
 * {@code space_id} en payload — le cloisonnement par espace se fait par filtre au retrieval.
 * L'upsert est géré par {@code QdrantVectorService} (ingestion-service).
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
