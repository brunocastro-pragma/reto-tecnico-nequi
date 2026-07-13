package co.com.nequi.franchise.r2dbc;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.r2dbc.adapter.BranchRepositoryAdapter;
import co.com.nequi.franchise.r2dbc.adapter.FranchiseRepositoryAdapter;
import co.com.nequi.franchise.r2dbc.adapter.ProductRepositoryAdapter;
import co.com.nequi.franchise.r2dbc.resilience.ResilienceDecorator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

/**
 * Runs against a real PostgreSQL: the top-stock query relies on DISTINCT ON, which H2 does not
 * implement, so an in-memory database would prove the code runs somewhere it never will.
 */
@Testcontainers
@DataR2dbcTest
@EnabledIf("dockerIsAvailable")
@Import({FranchiseRepositoryAdapter.class, BranchRepositoryAdapter.class, ProductRepositoryAdapter.class,
        ResilienceDecorator.class, R2dbcAdapterIntegrationTest.TestConfig.class})
class R2dbcAdapterIntegrationTest {

    // Skipped, not failed, when no Docker daemon is reachable: a red build nobody can fix locally
    // is a build people learn to ignore. CI always has Docker, so there it runs for real.
    static boolean dockerIsAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("franchise")
            .withUsername("franchise")
            .withPassword("franchise");

    @DynamicPropertySource
    static void r2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
                POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
            ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
            initializer.setConnectionFactory(connectionFactory);
            initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));
            return initializer;
        }

        @Bean
        CircuitBreakerRegistry circuitBreakerRegistry() {
            return CircuitBreakerRegistry.ofDefaults();
        }

        @Bean
        RetryRegistry retryRegistry() {
            return RetryRegistry.ofDefaults();
        }

        @Bean
        TimeLimiterRegistry timeLimiterRegistry() {
            // 10s: the first query pays for the container warming up, and timing out on that would
            // be testing Docker, not the adapter.
            return TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(10))
                    .build());
        }
    }

    @Autowired
    private FranchiseRepositoryAdapter franchiseAdapter;

    @Autowired
    private BranchRepositoryAdapter branchAdapter;

    @Autowired
    private ProductRepositoryAdapter productAdapter;

    private String franchiseId;

    @BeforeEach
    void setUp() {
        franchiseId = UUID.randomUUID().toString();
    }

    @Test
    void insertsAndReadsBackAFranchise() {
        StepVerifier.create(franchiseAdapter.create(franchise("Nequi Foods " + franchiseId))
                        .then(franchiseAdapter.findById(franchiseId)))
                .expectNextMatches(found -> found.getId().equals(franchiseId))
                .verifyComplete();
    }

    // Guards the create()/update() split: with a single save(), Spring Data would see a non-null
    // id, assume the row exists and emit an UPDATE matching nothing.
    @Test
    void updateChangesTheRowInsteadOfInsertingASecondOne() {
        Franchise original = franchise("Original " + franchiseId);

        StepVerifier.create(franchiseAdapter.create(original)
                        .map(saved -> saved.toBuilder().name("Renamed " + franchiseId).build())
                        .flatMap(franchiseAdapter::update)
                        .then(franchiseAdapter.findById(franchiseId)))
                .expectNextMatches(found -> found.getName().equals("Renamed " + franchiseId))
                .verifyComplete();
    }

    @Test
    void existsByNameReportsTheNameIsTaken() {
        StepVerifier.create(franchiseAdapter.create(franchise("Taken " + franchiseId))
                        .then(franchiseAdapter.existsByName("Taken " + franchiseId)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void findByIdIsEmptyWhenTheFranchiseIsUnknown() {
        StepVerifier.create(franchiseAdapter.findById("does-not-exist"))
                .verifyComplete();
    }

    // The branch with no products does not appear: the JOIN drops it, which is what we want --
    // a branch with no products has no top product.
    @Test
    void returnsTheHighestStockProductOfEachBranch() {
        String centro = UUID.randomUUID().toString();
        String poblado = UUID.randomUUID().toString();
        String empty = UUID.randomUUID().toString();

        Mono<Void> fixture = franchiseAdapter.create(franchise("Top " + franchiseId))
                .then(branchAdapter.create(branch(centro, "Centro")))
                .then(branchAdapter.create(branch(poblado, "Poblado")))
                .then(branchAdapter.create(branch(empty, "No products")))
                .then(productAdapter.create(product(centro, "Espresso", 90)))
                .then(productAdapter.create(product(centro, "Latte", 30)))
                .then(productAdapter.create(product(poblado, "Mocha", 150)))
                .then(productAdapter.create(product(poblado, "Tea", 149)))
                .then();

        StepVerifier.create(fixture.thenMany(productAdapter.findTopStockByFranchiseId(franchiseId))
                        .sort((left, right) -> left.getStock().compareTo(right.getStock())))
                .expectNextMatches(top -> top.getBranchName().equals("Centro")
                        && top.getProductName().equals("Espresso")
                        && top.getStock().equals(90))
                .expectNextMatches(top -> top.getBranchName().equals("Poblado")
                        && top.getProductName().equals("Mocha")
                        && top.getStock().equals(150))
                .verifyComplete();
    }

    @Test
    void deletesAProduct() {
        String branchId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();

        Mono<Void> fixture = franchiseAdapter.create(franchise("Delete " + franchiseId))
                .then(branchAdapter.create(branch(branchId, "Centro")))
                .then(productAdapter.create(Product.builder()
                        .id(productId).name("Espresso").stock(10).branchId(branchId).build()))
                .then();

        StepVerifier.create(fixture
                        .then(productAdapter.deleteById(productId))
                        .then(productAdapter.findById(productId)))
                .verifyComplete();
    }

    private Franchise franchise(String name) {
        return Franchise.builder().id(franchiseId).name(name).build();
    }

    private Branch branch(String id, String name) {
        return Branch.builder().id(id).name(name).franchiseId(franchiseId).build();
    }

    private Product product(String branchId, String name, int stock) {
        return Product.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .stock(stock)
                .branchId(branchId)
                .build();
    }
}
