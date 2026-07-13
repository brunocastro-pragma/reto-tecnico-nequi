package co.com.nequi.franchise.model.exception;

public sealed class DomainException extends RuntimeException
        permits ResourceNotFoundException, InvalidStockException, DuplicateFranchiseException {

    protected DomainException(String message) {
        super(message);
    }
}
