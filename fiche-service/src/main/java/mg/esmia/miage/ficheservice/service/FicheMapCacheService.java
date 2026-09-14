package mg.esmia.miage.ficheservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cache Redis des résumés intermédiaires produits par la phase MAP du pattern Map-Reduce.
 * <p>
 * Clé : {@code fiche:map:{spaceId}:{documentId}}
 * TTL : 24h (configurable via {@code fiche.map-cache-ttl-hours}).
 * Invalidation : sur DOCUMENT_READY (tout l'espace) ou SPACE_DELETED.
 * <p>
 * Partagé entre fiches et quiz (les deux lisent les mêmes chunks depuis Qdrant).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FicheMapCacheService {

    private static final String PREFIX = "fiche:map:";

    private final StringRedisTemplate redis;

    @Value("${fiche.map-cache-ttl-hours:24}")
    private long mapCacheTtlHours = 24;

    /**
     * Récupère un résumé intermédiaire mis en cache pour un document donné.
     */
    public Optional<String> get(UUID spaceId, UUID documentId) {
        String key = key(spaceId, documentId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            log.debug("MAP cache hit : space={}, doc={}", spaceId, documentId);
        }
        return Optional.ofNullable(cached);
    }

    /**
     * Met en cache un résumé intermédiaire produit par la phase MAP.
     */
    public void put(UUID spaceId, UUID documentId, String summary) {
        String key = key(spaceId, documentId);
        redis.opsForValue().set(key, summary, Duration.ofHours(mapCacheTtlHours));
        log.debug("MAP cache put : space={}, doc={}, length={}", spaceId, documentId, summary.length());
    }

    /**
     * Invalide tous les résumés MAP d'un espace (appelé sur DOCUMENT_READY).
     */
    public void invalidateSpace(UUID spaceId) {
        String pattern = PREFIX + spaceId + ":*";
        Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.info("MAP cache invalidé pour l'espace {} ({} clés supprimées)", spaceId, keys.size());
        }
    }

    /**
     * Invalide un document spécifique (appelé si ré-ingestion ciblée).
     */
    public void invalidateDocument(UUID spaceId, UUID documentId) {
        String key = key(spaceId, documentId);
        Boolean deleted = redis.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("MAP cache invalidé : space={}, doc={}", spaceId, documentId);
        }
    }

    private String key(UUID spaceId, UUID documentId) {
        return PREFIX + spaceId + ":" + documentId;
    }
}
