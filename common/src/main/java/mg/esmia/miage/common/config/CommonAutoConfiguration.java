package mg.esmia.miage.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mg.esmia.miage.common.context.UserContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration du module common : dès que ce jar est sur le classpath d'un
 * service (voir META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports),
 * le filtre de contexte utilisateur et le mapper JSON sont enregistrés automatiquement.
 * GlobalExceptionHandler est importé séparément (voir le même fichier .imports).
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
