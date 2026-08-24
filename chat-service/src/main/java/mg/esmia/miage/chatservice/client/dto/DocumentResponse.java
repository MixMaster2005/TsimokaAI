package mg.esmia.miage.chatservice.client.dto;

import java.util.UUID;

/**
 * Copie locale minimale de {@code ingestion-service/dto/DocumentResponse} — seuls les
 * champs utiles à chat-service sont mappés (Jackson ignore les autres, et Spring Boot
 * ne échoue pas sur les propriétés inconnues).
 */
public record DocumentResponse(UUID id, UUID spaceId, UUID userId, String filename) {
}
