package co.com.nequi.franchise.r2dbc.adapter;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.r2dbc.entity.BranchEntity;
import co.com.nequi.franchise.r2dbc.entity.FranchiseEntity;
import co.com.nequi.franchise.r2dbc.entity.ProductEntity;
import co.com.nequi.franchise.r2dbc.entity.TopStockRow;
import co.com.nequi.franchise.r2dbc.repository.BranchR2dbcRepository;
import co.com.nequi.franchise.r2dbc.repository.FranchiseR2dbcRepository;
import co.com.nequi.franchise.r2dbc.repository.ProductR2dbcRepository;
import co.com.nequi.franchise.r2dbc.resilience.ResilienceDecorator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the adapters: the repository and the template are mocked, so what is under test
 * is the translation between domain objects and persistence entities, and the fact that create()
 * inserts while update() updates.
 *
 * The database itself is covered by R2dbcAdapterIntegrationTest, against a real PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryAdaptersTest {

    private static final String FRANCHISE_ID = "f-1";
    private static final String BRANCH_ID = "b-1";
    private static final String PRODUCT_ID = "p-1";

    @Mock
    private FranchiseR2dbcRepository franchiseRepository;
    @Mock
    private BranchR2dbcRepository branchRepository;
    @Mock
    private ProductR2dbcRepository productRepository;
    @Mock
    private R2dbcEntityTemplate template;

    private FranchiseRepositoryAdapter franchiseAdapter;
    private BranchRepositoryAdapter branchAdapter;
    private ProductRepositoryAdapter productAdapter;

    @BeforeEach
    void setUp() {
        // A real decorator, not a mock: the adapters must keep working through it, and this is the
        // cheapest place to prove the operators do not swallow or alter the signals.
        ResilienceDecorator resilience = new ResilienceDecorator(
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build()));

        franchiseAdapter = new FranchiseRepositoryAdapter(franchiseRepository, template, resilience);
        branchAdapter = new BranchRepositoryAdapter(branchRepository, template, resilience);
        productAdapter = new ProductRepositoryAdapter(productRepository, template, resilience);
    }

    // --- franchise -----------------------------------------------------------

    @Test
    void createFranchiseInsertsAndMapsBackToTheDomain() {
        FranchiseEntity entity = FranchiseEntity.builder().id(FRANCHISE_ID).name("Nequi Foods").build();
        when(template.insert(any(FranchiseEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(franchiseAdapter.create(
                        Franchise.builder().id(FRANCHISE_ID).name("Nequi Foods").build()))
                .expectNextMatches(franchise -> franchise.getId().equals(FRANCHISE_ID)
                        && franchise.getName().equals("Nequi Foods"))
                .verifyComplete();

        verify(template).insert(any(FranchiseEntity.class));
    }

    @Test
    void updateFranchiseUsesUpdateAndNotInsert() {
        FranchiseEntity entity = FranchiseEntity.builder().id(FRANCHISE_ID).name("Renamed").build();
        when(template.update(any(FranchiseEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(franchiseAdapter.update(
                        Franchise.builder().id(FRANCHISE_ID).name("Renamed").build()))
                .expectNextMatches(franchise -> franchise.getName().equals("Renamed"))
                .verifyComplete();

        verify(template).update(any(FranchiseEntity.class));
    }

    @Test
    void findFranchiseByIdIsEmptyWhenTheRowDoesNotExist() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(franchiseAdapter.findById(FRANCHISE_ID))
                .verifyComplete();
    }

    @Test
    void findFranchiseByIdMapsTheEntity() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(
                FranchiseEntity.builder().id(FRANCHISE_ID).name("Nequi Foods").build()));

        StepVerifier.create(franchiseAdapter.findById(FRANCHISE_ID))
                .expectNextMatches(franchise -> franchise.getName().equals("Nequi Foods"))
                .verifyComplete();
    }

    @Test
    void existsByNameDelegatesToTheRepository() {
        when(franchiseRepository.existsByName("Nequi Foods")).thenReturn(Mono.just(true));

        StepVerifier.create(franchiseAdapter.existsByName("Nequi Foods"))
                .expectNext(true)
                .verifyComplete();
    }

    // --- branch --------------------------------------------------------------

    @Test
    void createBranchKeepsTheFranchiseItBelongsTo() {
        BranchEntity entity = BranchEntity.builder()
                .id(BRANCH_ID).name("Centro").franchiseId(FRANCHISE_ID).build();
        when(template.insert(any(BranchEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(branchAdapter.create(
                        Branch.builder().id(BRANCH_ID).name("Centro").franchiseId(FRANCHISE_ID).build()))
                .expectNextMatches(branch -> branch.getFranchiseId().equals(FRANCHISE_ID)
                        && branch.getName().equals("Centro"))
                .verifyComplete();
    }

    @Test
    void updateBranchUsesUpdate() {
        BranchEntity entity = BranchEntity.builder()
                .id(BRANCH_ID).name("Poblado").franchiseId(FRANCHISE_ID).build();
        when(template.update(any(BranchEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(branchAdapter.update(
                        Branch.builder().id(BRANCH_ID).name("Poblado").franchiseId(FRANCHISE_ID).build()))
                .expectNextMatches(branch -> branch.getName().equals("Poblado"))
                .verifyComplete();

        verify(template).update(any(BranchEntity.class));
    }

    @Test
    void findBranchByIdMapsTheEntity() {
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Mono.just(
                BranchEntity.builder().id(BRANCH_ID).name("Centro").franchiseId(FRANCHISE_ID).build()));

        StepVerifier.create(branchAdapter.findById(BRANCH_ID))
                .expectNextMatches(branch -> branch.getId().equals(BRANCH_ID))
                .verifyComplete();
    }

    // --- product -------------------------------------------------------------

    @Test
    void createProductCarriesNameStockAndBranch() {
        ProductEntity entity = ProductEntity.builder()
                .id(PRODUCT_ID).name("Espresso").stock(40).branchId(BRANCH_ID).build();
        when(template.insert(any(ProductEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(productAdapter.create(Product.builder()
                        .id(PRODUCT_ID).name("Espresso").stock(40).branchId(BRANCH_ID).build()))
                .expectNextMatches(product -> product.getStock().equals(40)
                        && product.getBranchId().equals(BRANCH_ID))
                .verifyComplete();
    }

    @Test
    void updateProductUsesUpdate() {
        ProductEntity entity = ProductEntity.builder()
                .id(PRODUCT_ID).name("Espresso").stock(200).branchId(BRANCH_ID).build();
        when(template.update(any(ProductEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(productAdapter.update(Product.builder()
                        .id(PRODUCT_ID).name("Espresso").stock(200).branchId(BRANCH_ID).build()))
                .expectNextMatches(product -> product.getStock().equals(200))
                .verifyComplete();

        verify(template).update(any(ProductEntity.class));
    }

    @Test
    void findProductByIdMapsTheEntity() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Mono.just(
                ProductEntity.builder().id(PRODUCT_ID).name("Espresso").stock(40).branchId(BRANCH_ID).build()));

        StepVerifier.create(productAdapter.findById(PRODUCT_ID))
                .expectNextMatches(product -> product.getName().equals("Espresso"))
                .verifyComplete();
    }

    @Test
    void deleteProductCompletesEmpty() {
        when(productRepository.deleteById(PRODUCT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(productAdapter.deleteById(PRODUCT_ID))
                .verifyComplete();

        verify(productRepository).deleteById(PRODUCT_ID);
    }

    @Test
    void topStockRowsAreMappedToTheDomainProjection() {
        when(productRepository.findTopStockByFranchiseId(FRANCHISE_ID)).thenReturn(Flux.just(
                new TopStockRow(BRANCH_ID, "Centro", PRODUCT_ID, "Espresso", 200),
                new TopStockRow("b-2", "Poblado", "p-9", "Mocha", 150)));

        StepVerifier.create(productAdapter.findTopStockByFranchiseId(FRANCHISE_ID))
                .expectNextMatches(top -> top.getBranchName().equals("Centro")
                        && top.getProductName().equals("Espresso")
                        && top.getStock().equals(200))
                .expectNextMatches(top -> top.getBranchName().equals("Poblado")
                        && top.getStock().equals(150))
                .verifyComplete();
    }
}
