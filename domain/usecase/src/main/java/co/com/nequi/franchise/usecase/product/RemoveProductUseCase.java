package co.com.nequi.franchise.usecase.product;

import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import co.com.nequi.franchise.usecase.shared.OwnershipResolver;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RemoveProductUseCase {

    private final OwnershipResolver ownershipResolver;
    private final ProductRepository productRepository;

    public Mono<Void> execute(String franchiseId, String branchId, String productId) {
        return ownershipResolver.productOfBranch(franchiseId, branchId, productId)
                .map(Product::getId)
                .flatMap(productRepository::deleteById);
    }
}
