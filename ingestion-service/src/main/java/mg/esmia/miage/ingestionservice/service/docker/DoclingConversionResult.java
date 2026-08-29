package mg.esmia.miage.ingestionservice.service.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Miroir de la réponse JSON de docling-worker (POST /v1/convert).
 *
 * <p>⚠️ {@code pages_processed} (snake_case côté Python) doit être mappé via
 * {@link JsonProperty @JsonProperty("pages_processed")} sur le champ camelCase
 * {@code pagesProcessed} du record — sinon Jackson le désérialiserait silencieusement à 0.
 * Idem pour les champs snake_case de {@link DoclingImage}.</p>
 *
 * <p>Le champ {@code document} contient l'AST canonique (représentation structurée du document).
 * Il est typé comme {@link JsonNode} car l'AST est encore en évolution ; on le typera
 * proprement une fois le contrat stabilisé.</p>
 */
public record DoclingConversionResult(
        JsonNode document,
        String markdown,
        String method,
        @JsonProperty("pages_processed") int pagesProcessed,
        List<String> warnings,
        List<DoclingImage> images) {

    public DoclingConversionResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        images = images == null ? List.of() : List.copyOf(images);
    }

    /**
     * Image extraite par docling-worker (spec v2) : renvoyée en base64 + légende Gemini.
     * `ingestion-service` l'uploade dans MinIO puis substitue {@code {{IMAGE:img_001}}}
     * dans le Markdown par {@code ![caption](url)}.
     */
    public record DoclingImage(
            @JsonProperty("placeholder_id") String placeholderId,
            @JsonProperty("content_type") String contentType,
            @JsonProperty("data_base64") String dataBase64,
            String caption) {
    }
}
