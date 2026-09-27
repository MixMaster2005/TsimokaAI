package mg.esmia.miage.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Déduplication des événements Redis at-least-once (Lot 3).
 *
 * <p>SETNX + TTL : premier traitement → {@code true} ; redélivrance sous TTL →
 * {@code false} (compteurs non rejoués). Fail-open : si Redis est indisponible,
 * retourne {@code true} (traite quand même) pour ne jamais bloquer l'analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventDedupService {

    static final Duration DEDUP_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public boolean tryMarkProcessed(String key) {
        try {
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, "1", DEDUP_TTL);
            return !Boolean.FALSE.equals(absent);
        } catch (Exception e) {
            log.warn("Dedup Redis indisponible (clé {}) : traitement sans déduplication.", key, e);
            return true;
        }
    }
}
