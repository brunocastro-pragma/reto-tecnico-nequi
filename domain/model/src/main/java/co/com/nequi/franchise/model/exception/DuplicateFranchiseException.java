package co.com.nequi.franchise.model.exception;

public final class DuplicateFranchiseException extends DomainException {

    public DuplicateFranchiseException(String name) {
        super("A franchise named [%s] already exists".formatted(name));
    }
}
