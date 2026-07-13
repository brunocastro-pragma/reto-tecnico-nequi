package co.com.nequi.franchise.config;

import co.com.nequi.franchise.model.branch.gateways.BranchRepository;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import co.com.nequi.franchise.usecase.branch.AddBranchUseCase;
import co.com.nequi.franchise.usecase.branch.UpdateBranchNameUseCase;
import co.com.nequi.franchise.usecase.franchise.CreateFranchiseUseCase;
import co.com.nequi.franchise.usecase.franchise.UpdateFranchiseNameUseCase;
import co.com.nequi.franchise.usecase.product.AddProductUseCase;
import co.com.nequi.franchise.usecase.product.GetTopStockProductsUseCase;
import co.com.nequi.franchise.usecase.product.RemoveProductUseCase;
import co.com.nequi.franchise.usecase.product.UpdateProductNameUseCase;
import co.com.nequi.franchise.usecase.product.UpdateProductStockUseCase;
import co.com.nequi.franchise.usecase.shared.OwnershipResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Use cases are wired here rather than annotated with @Service: annotating them would put Spring
 * on the domain's classpath, and domain/usecase depends on nothing but the model.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public OwnershipResolver ownershipResolver(BranchRepository branchRepository,
                                               ProductRepository productRepository) {
        return new OwnershipResolver(branchRepository, productRepository);
    }

    @Bean
    public CreateFranchiseUseCase createFranchiseUseCase(FranchiseRepository franchiseRepository) {
        return new CreateFranchiseUseCase(franchiseRepository);
    }

    @Bean
    public UpdateFranchiseNameUseCase updateFranchiseNameUseCase(FranchiseRepository franchiseRepository) {
        return new UpdateFranchiseNameUseCase(franchiseRepository);
    }

    @Bean
    public AddBranchUseCase addBranchUseCase(FranchiseRepository franchiseRepository,
                                             BranchRepository branchRepository) {
        return new AddBranchUseCase(franchiseRepository, branchRepository);
    }

    @Bean
    public UpdateBranchNameUseCase updateBranchNameUseCase(OwnershipResolver ownershipResolver,
                                                           BranchRepository branchRepository) {
        return new UpdateBranchNameUseCase(ownershipResolver, branchRepository);
    }

    @Bean
    public AddProductUseCase addProductUseCase(OwnershipResolver ownershipResolver,
                                               ProductRepository productRepository) {
        return new AddProductUseCase(ownershipResolver, productRepository);
    }

    @Bean
    public RemoveProductUseCase removeProductUseCase(OwnershipResolver ownershipResolver,
                                                     ProductRepository productRepository) {
        return new RemoveProductUseCase(ownershipResolver, productRepository);
    }

    @Bean
    public UpdateProductStockUseCase updateProductStockUseCase(OwnershipResolver ownershipResolver,
                                                               ProductRepository productRepository) {
        return new UpdateProductStockUseCase(ownershipResolver, productRepository);
    }

    @Bean
    public UpdateProductNameUseCase updateProductNameUseCase(OwnershipResolver ownershipResolver,
                                                             ProductRepository productRepository) {
        return new UpdateProductNameUseCase(ownershipResolver, productRepository);
    }

    @Bean
    public GetTopStockProductsUseCase getTopStockProductsUseCase(FranchiseRepository franchiseRepository,
                                                                 ProductRepository productRepository) {
        return new GetTopStockProductsUseCase(franchiseRepository, productRepository);
    }
}
