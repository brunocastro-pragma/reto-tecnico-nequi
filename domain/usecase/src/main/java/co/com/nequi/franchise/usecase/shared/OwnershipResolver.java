package co.com.nequi.franchise.usecase.shared;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.branch.gateways.BranchRepository;
import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Routes are nested, so every product operation must prove the franchise -> branch -> product
 * chain before touching anything. Shared by the four use cases that need it.
 */
@RequiredArgsConstructor
public class OwnershipResolver {

    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    public Mono<Branch> branchOfFranchise(String franchiseId, String branchId) {
        return branchRepository.findById(branchId)
                .filter(branch -> branch.getFranchiseId().equals(franchiseId))
                .switchIfEmpty(Mono.error(ResourceNotFoundException.branch(branchId)));
    }

    public Mono<Product> productOfBranch(String franchiseId, String branchId, String productId) {
        return branchOfFranchise(franchiseId, branchId)
                .flatMap(branch -> productRepository.findById(productId))
                .filter(product -> product.getBranchId().equals(branchId))
                .switchIfEmpty(Mono.error(ResourceNotFoundException.product(productId)));
    }
}
