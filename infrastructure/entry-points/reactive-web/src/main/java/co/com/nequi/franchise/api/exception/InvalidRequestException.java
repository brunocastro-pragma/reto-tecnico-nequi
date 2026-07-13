package co.com.nequi.franchise.api.exception;

/** Transport-level validation failure. Not a DomainException: a malformed payload is not a broken business rule. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
