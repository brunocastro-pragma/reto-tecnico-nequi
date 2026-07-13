package co.com.nequi.franchise.usecase.product;

import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import co.com.nequi.franchise.usecase.shared.OwnershipResolver;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class AddProductUseCase {

    private final OwnershipResolver ownershipResolver;
    private final ProductRepository productRepository;

    public Mono<Product> execute(String franchiseId, String branchId, String name, Integer stock) {
        return StockRule.validate(stock)
                .flatMap(validStock -> ownershipResolver.branchOfFranchise(franchiseId, branchId)
                        .map(branch -> Product.builder()
                                .id(UUID.randomUUID().toString())
                                .name(name)
                                .stock(validStock)
                                .branchId(branch.getId())
                                .build()))
                .flatMap(productRepository::create);
    }
}
