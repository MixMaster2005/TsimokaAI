package mg.esmia.miage.common.exception;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message, 409);
    }
}
