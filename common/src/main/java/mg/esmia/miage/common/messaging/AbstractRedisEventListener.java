package mg.esmia.miage.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Base réutilisable pour les listeners Redis Pub/Sub des différents services.
 * Chaque service concret précise le type d'événement (record) et implémente
 * onEvent(...) de façon IDEMPOTENTE, comme l'exige le contrat d'événements.
 *
 * Câblage (à faire dans chaque service, dans une @Configuration) :
 * <pre>
 *   RedisMessageListenerContainer container = new RedisMessageListenerContainer();
 *   container.setConnectionFactory(connectionFactory);
 *   container.addMessageListener(myListener, new ChannelTopic(EventChannels.CHAT_EVENTS));
 * </pre>
 */
@Slf4j
public abstract class AbstractRedisEventListener<T> implements MessageListener {

    private final ObjectMapper objectMapper;
    private final Class<T> eventType;

    protected AbstractRedisEventListener(ObjectMapper objectMapper, Class<T> eventType) {
        this.objectMapper = objectMapper;
        this.eventType = eventType;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            T event = objectMapper.readValue(message.getBody(), eventType);
            onEvent(event);
        } catch (Exception e) {
            log.error("Impossible de désérialiser/traiter l'événement reçu sur {}",
                    new String(message.getChannel()), e);
        }
    }

    protected abstract void onEvent(T event);
}
