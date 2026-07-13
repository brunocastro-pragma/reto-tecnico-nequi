package co.com.nequi.franchise.usecase.product;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.branch.gateways.BranchRepository;
import co.com.nequi.franchise.model.exception.InvalidStockException;
import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.TopStockProduct;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import co.com.nequi.franchise.usecase.shared.OwnershipResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUseCasesTest {

    private static final String FRANCHISE_ID = "f-1";
    private static final String BRANCH_ID = "b-1";
    private static final String PRODUCT_ID = "p-1";

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    private OwnershipResolver ownershipResolver;

    @BeforeEach
    void setUp() {
        ownershipResolver = new OwnershipResolver(branchRepository, productRepository);
    }

    @Test
    void addsProductToABranchOfTheFranchise() {
        AddProductUseCase useCase = new AddProductUseCase(ownershipResolver, productRepository);
        givenBranchExists();
        when(productRepository.create(any(Product.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Espresso", 40))
                .assertNext(product -> {
                    assertThat(product.getId()).isNotBlank();
                    assertThat(product.getName()).isEqualTo("Espresso");
                    assertThat(product.getStock()).isEqualTo(40);
                    assertThat(product.getBranchId()).isEqualTo(BRANCH_ID);
                })
                .verifyComplete();
    }

    // StockRule sits first in the chain, so an invalid request costs zero database round-trips.
    @Test
    void refusesNegativeStockWithoutTouchingTheDatabase() {
        AddProductUseCase useCase = new AddProductUseCase(ownershipResolver, productRepository);

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Espresso", -1))
                .expectError(InvalidStockException.class)
                .verify();

        verify(branchRepository, never()).findById(anyString());
        verify(productRepository, never()).create(any());
    }

    @Test
    void refusesToAddProductToABranchOfAnotherFranchise() {
        AddProductUseCase useCase = new AddProductUseCase(ownershipResolver, productRepository);
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Mono.just(
                Branch.builder().id(BRANCH_ID).name("Other").franchiseId("another-franchise").build()));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Espresso", 40))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(productRepository, never()).create(any());
    }

    @Test
    void updatesStockOfAnExistingProduct() {
        UpdateProductStockUseCase useCase = new UpdateProductStockUseCase(ownershipResolver, productRepository);
        givenBranchExists();
        givenProductExists();
        when(productRepository.update(any(Product.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 120))
                .expectNextMatches(product -> product.getStock().equals(120)
                        && product.getName().equals("Espresso"))
                .verifyComplete();
    }

    @Test
    void refusesNegativeStockOnUpdate() {
        UpdateProductStockUseCase useCase = new UpdateProductStockUseCase(ownershipResolver, productRepository);

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, -5))
                .expectError(InvalidStockException.class)
                .verify();

        verify(productRepository, never()).update(any());
    }

    @Test
    void renamesAnExistingProduct() {
        UpdateProductNameUseCase useCase = new UpdateProductNameUseCase(ownershipResolver, productRepository);
        givenBranchExists();
        givenProductExists();
        when(productRepository.update(any(Product.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Ristretto"))
                .expectNextMatches(product -> product.getName().equals("Ristretto")
                        && product.getStock().equals(40))
                .verifyComplete();
    }

    @Test
    void removesAnExistingProduct() {
        RemoveProductUseCase useCase = new RemoveProductUseCase(ownershipResolver, productRepository);
        givenBranchExists();
        givenProductExists();
        when(productRepository.deleteById(PRODUCT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .verifyComplete();

        verify(productRepository).deleteById(PRODUCT_ID);
    }

    @Test
    void refusesToRemoveAProductOfAnotherBranch() {
        RemoveProductUseCase useCase = new RemoveProductUseCase(ownershipResolver, productRepository);
        givenBranchExists();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Mono.just(
                Product.builder().id(PRODUCT_ID).name("Espresso").stock(40).branchId("another-branch").build()));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(productRepository, never()).deleteById(anyString());
    }

    @Test
    void returnsOneTopProductPerBranch() {
        GetTopStockProductsUseCase useCase =
                new GetTopStockProductsUseCase(franchiseRepository, productRepository);
        when(franchiseRepository.findById(FRANCHISE_ID))
                .thenReturn(Mono.just(Franchise.builder().id(FRANCHISE_ID).name("Nequi Foods").build()));
        when(productRepository.findTopStockByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.just(
                topStock("b-1", "Centro", "p-1", "Espresso", 90),
                topStock("b-2", "Poblado", "p-9", "Latte", 150)));

        StepVerifier.create(useCase.execute(FRANCHISE_ID))
                .expectNextMatches(top -> top.getBranchId().equals("b-1") && top.getStock().equals(90))
                .expectNextMatches(top -> top.getBranchId().equals("b-2") && top.getStock().equals(150))
                .verifyComplete();
    }

    @Test
    void failsTopStockWhenFranchiseDoesNotExist() {
        GetTopStockProductsUseCase useCase =
                new GetTopStockProductsUseCase(franchiseRepository, productRepository);
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(FRANCHISE_ID))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(productRepository, never()).findTopStockByFranchiseId(anyString());
    }

    private void givenBranchExists() {
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Mono.just(
                Branch.builder().id(BRANCH_ID).name("Centro").franchiseId(FRANCHISE_ID).build()));
    }

    private void givenProductExists() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Mono.just(
                Product.builder().id(PRODUCT_ID).name("Espresso").stock(40).branchId(BRANCH_ID).build()));
    }

    private TopStockProduct topStock(String branchId, String branchName,
                                     String productId, String productName, int stock) {
        return TopStockProduct.builder()
                .branchId(branchId)
                .branchName(branchName)
                .productId(productId)
                .productName(productName)
                .stock(stock)
                .build();
    }
}
