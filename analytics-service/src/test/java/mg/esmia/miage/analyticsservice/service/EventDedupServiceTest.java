package mg.esmia.miage.analyticsservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Lot 3 : déduplication SETNX+TTL (premier passage true, redélivrance false,
 * fail-open si Redis indisponible).
 */
@ExtendWith(MockitoExtension.class)
class EventDedupServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    EventDedupService dedupService;

    @Test
    void premierTraitement_retourneVrai() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("k"), eq("1"), any(Duration.class))).thenReturn(true);
        assertTrue(dedupService.tryMarkProcessed("k"));
    }

    @Test
    void redelivrance_retourneFaux() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("k"), eq("1"), any(Duration.class))).thenReturn(false);
        assertFalse(dedupService.tryMarkProcessed("k"));
    }

    @Test
    void redisIndisponible_failOpen() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("connexion perdue"));
        assertTrue(dedupService.tryMarkProcessed("k"));
    }
}
