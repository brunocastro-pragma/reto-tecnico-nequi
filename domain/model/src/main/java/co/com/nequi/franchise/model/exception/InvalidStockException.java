package co.com.nequi.franchise.model.exception;

public final class InvalidStockException extends DomainException {

    public InvalidStockException(Integer stock) {
        super("Stock must be zero or positive, but was [%s]".formatted(stock));
    }
}
