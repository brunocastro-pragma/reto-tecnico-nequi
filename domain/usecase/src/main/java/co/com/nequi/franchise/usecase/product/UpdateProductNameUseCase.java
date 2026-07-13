package co.com.nequi.franchise.usecase.product;

import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import co.com.nequi.franchise.usecase.shared.OwnershipResolver;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UpdateProductNameUseCase {

    private final OwnershipResolver ownershipResolver;
    private final ProductRepository productRepository;

    public Mono<Product> execute(String franchiseId, String branchId, String productId, String newName) {
        return ownershipResolver.productOfBranch(franchiseId, branchId, productId)
                .map(product -> product.toBuilder().name(newName).build())
                .flatMap(productRepository::update);
    }
}
