package mg.esmia.miage.common.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message, 400);
    }
}
