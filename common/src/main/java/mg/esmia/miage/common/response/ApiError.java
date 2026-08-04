package mg.esmia.miage.common.response;

import java.util.Map;

/** Corps d'erreur uniforme : { code, message, details }. */
public record ApiError(String code, String message, Map<String, Object> details) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of());
    }

    public static ApiError of(String code, String message, Map<String, Object> details) {
        return new ApiError(code, message, details == null ? Map.of() : details);
    }
}
