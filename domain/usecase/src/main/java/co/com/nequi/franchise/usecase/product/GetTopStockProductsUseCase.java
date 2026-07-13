package co.com.nequi.franchise.usecase.product;

import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import co.com.nequi.franchise.model.product.TopStockProduct;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetTopStockProductsUseCase {

    private final FranchiseRepository franchiseRepository;
    private final ProductRepository productRepository;

    public Flux<TopStockProduct> execute(String franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(ResourceNotFoundException.franchise(franchiseId)))
                .map(Franchise::getId)
                .flatMapMany(productRepository::findTopStockByFranchiseId);
    }
}
