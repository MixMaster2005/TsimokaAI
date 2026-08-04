package mg.esmia.miage.common.response;

/**
 * Enveloppe de réponse uniforme utilisée par TOUS les services (contrat non négociable).
 *
 * Succès : { "success": true,  "data": {...},  "meta": {...} }
 * Erreur  : { "success": false, "error": {...}, "meta": {...} }
 */
public record ApiResponse<T>(boolean success, T data, ApiError error, Meta meta) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, data, null, Meta.now(requestId));
    }

    public static <T> ApiResponse<T> error(ApiError error, String requestId) {
        return new ApiResponse<>(false, null, error, Meta.now(requestId));
    }
}
