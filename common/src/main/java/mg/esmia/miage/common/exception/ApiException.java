package mg.esmia.miage.common.exception;

import lombok.Getter;

/**
 * Exception métier de base. Toutes les exceptions applicatives des services
 * devraient en hériter afin d'être automatiquement converties par GlobalExceptionHandler
 * au format d'enveloppe d'erreur uniforme.
 */
@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public ApiException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
