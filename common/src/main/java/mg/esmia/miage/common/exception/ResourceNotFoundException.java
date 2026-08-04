package mg.esmia.miage.common.exception;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message, 404);
    }
}
