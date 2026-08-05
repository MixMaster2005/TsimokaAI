package mg.esmia.miage.ingestionservice.service.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client Docker vers le daemon de l'hôte, via la lib docker-java (transport HTTPClient5).
 * Le socket n'est contacté qu'au premier appel réel : un daemon indisponible ne fait pas
 * échouer le démarrage du service — seul un convert() le déclenchera (document -> FAILED).
 */
@Configuration
@EnableConfigurationProperties(DockerWorkerProperties.class)
public class DockerClientBeanConfig {

    @Bean(destroyMethod = "close")
    public DockerClient dockerClient(DockerWorkerProperties properties) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(properties.getDockerHost())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(5)
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}
