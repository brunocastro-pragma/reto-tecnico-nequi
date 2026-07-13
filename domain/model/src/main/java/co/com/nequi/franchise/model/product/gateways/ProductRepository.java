package co.com.nequi.franchise.model.product.gateways;

import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.TopStockProduct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepository {

    Mono<Product> create(Product product);

    Mono<Product> update(Product product);

    Mono<Product> findById(String id);

    Mono<Void> deleteById(String id);

    Flux<TopStockProduct> findTopStockByFranchiseId(String franchiseId);
}
