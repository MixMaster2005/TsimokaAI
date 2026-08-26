package mg.esmia.miage.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mg.esmia.miage.common.context.UserContextFilter;
import mg.esmia.miage.common.messaging.RedisEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configuration du module common : dès que ce jar est sur le classpath d'un
 * service (voir META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports),
 * le filtre de contexte utilisateur, le mapper JSON et le publisher d'événements sont
 * enregistrés automatiquement. GlobalExceptionHandler est importé séparément (voir le
 * même fichier .imports).
 *
 * ⚠️ Tout bean de {@code common} destiné aux services DOIT être déclaré ici : les
 * packages de {@code common} ne sont pas couverts par leur component-scan.
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /** Publisher Redis Pub/Sub des événements inter-services (cf. package events). */
    @Bean
    @ConditionalOnMissingBean
    public RedisEventPublisher redisEventPublisher(StringRedisTemplate redisTemplate,
                                                   ObjectMapper objectMapper) {
        return new RedisEventPublisher(redisTemplate, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<UserContextFilter> userContextFilter() {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserContextFilter());
        registration.addUrlPatterns("/*");
        registration.setName("userContextFilter");
        registration.setOrder(1);
        return registration;
    }
}
