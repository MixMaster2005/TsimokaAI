package mg.esmia.miage.ingestionservice.service.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Miroir de la réponse JSON de docling-worker (POST /v1/convert).
 *
 * <p>⚠️ {@code pages_processed} (snake_case côté Python) doit être mappé via
 * {@link JsonProperty @JsonProperty("pages_processed")} sur le champ camelCase
 * {@code pagesProcessed} du record — sinon Jackson le désérialiserait silencieusement à 0.
 */
public record DoclingConversionResult(
        String markdown,
        String method,
        @JsonProperty("pages_processed") int pagesProcessed,
        List<String> warnings) {

    public DoclingConversionResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
