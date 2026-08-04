package mg.esmia.miage.spaceservice.config;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.spaceservice.messaging.IngestionEventListener;
import mg.esmia.miage.spaceservice.messaging.UserEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Câble les listeners Redis Pub/Sub de ce service sur leurs canaux respectifs.
 */
@Configuration
@RequiredArgsConstructor
public class RedisListenerConfig {

    private final IngestionEventListener ingestionEventListener;
    private final UserEventListener userEventListener;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(ingestionEventListener, new ChannelTopic(EventChannels.INGESTION_EVENTS));
        container.addMessageListener(userEventListener, new ChannelTopic(EventChannels.USER_EVENTS));
        return container;
    }
}
