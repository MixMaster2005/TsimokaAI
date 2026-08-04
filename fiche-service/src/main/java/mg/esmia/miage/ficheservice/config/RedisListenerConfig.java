package mg.esmia.miage.ficheservice.config;

import lombok.RequiredArgsConstructor;
import mg.esmia.miage.common.events.EventChannels;
import mg.esmia.miage.ficheservice.messaging.IngestionEventListener;
import mg.esmia.miage.ficheservice.messaging.SpaceEventListener;
import mg.esmia.miage.ficheservice.messaging.UserEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisListenerConfig {

    private final IngestionEventListener ingestionEventListener;
    private final SpaceEventListener spaceEventListener;
    private final UserEventListener userEventListener;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(ingestionEventListener, new ChannelTopic(EventChannels.INGESTION_EVENTS));
        container.addMessageListener(spaceEventListener, new ChannelTopic(EventChannels.SPACE_EVENTS));
        container.addMessageListener(userEventListener, new ChannelTopic(EventChannels.USER_EVENTS));
        return container;
    }
}
