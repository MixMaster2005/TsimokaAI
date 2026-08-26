package mg.esmia.miage.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import mg.esmia.miage.common.context.UserContext;
import mg.esmia.miage.common.response.ApiError;
import mg.esmia.miage.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduit toute exception en enveloppe { success:false, error, meta } uniforme.
 * Auto-enregistré dans chaque service via META-INF/spring/...AutoConfiguration.imports.
 *
 * ⚠️ Le catch-all LOGGE la stacktrace : une 500 silencieuse est indébuggable.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String requestId(HttpServletRequest request) {
        String rid = request.getHeader(UserContext.HEADER_REQUEST_ID);
        return (rid == null || rid.isBlank()) ? UUID.randomUUID().toString() : rid;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiError error = ApiError.of(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(error, requestId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        ApiError error = ApiError.of(ErrorCode.VALIDATION_ERROR, "Requête invalide", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, requestId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erreur interne [{} {}] : {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        ApiError error = ApiError.of(ErrorCode.INTERNAL_ERROR, "Erreur interne inattendue");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error, requestId(request)));
    }
}
