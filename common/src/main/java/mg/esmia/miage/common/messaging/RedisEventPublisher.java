package mg.esmia.miage.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Publisher générique Redis Pub/Sub. Chaque service publie ses événements
 * (voir EventChannels + records dans le package events) en JSON brut sur le canal concerné.
 *
 * Enregistré par {@code CommonAutoConfiguration} (et non via @Component) : les packages
 * de {@code common} ne sont PAS couverts par le component-scan par défaut des services
 * (@SpringBootApplication ne scanne que son propre package) — un @Component ici serait
 * silencieusement ignoré et l'injection échouerait au démarrage.
 */
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String channel, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, payload);
            log.debug("Événement publié sur [{}] : {}", channel, payload);
        } catch (Exception e) {
            // Un échec de publication ne doit jamais faire planter la transaction métier
            // principale ; il est journalisé pour investigation (cf. contrats de qualité).
            log.error("Échec de publication de l'événement sur le canal {}", channel, e);
        }
    }
}
