package co.com.nequi.franchise.usecase.product;

import co.com.nequi.franchise.model.exception.InvalidStockException;
import reactor.core.publisher.Mono;

final class StockRule {

    private StockRule() {
    }

    static Mono<Integer> validate(Integer stock) {
        return Mono.justOrEmpty(stock)
                .filter(value -> value >= 0)
                .switchIfEmpty(Mono.error(new InvalidStockException(stock)));
    }
}
