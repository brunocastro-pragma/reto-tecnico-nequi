package co.com.nequi.franchise.api;

import co.com.nequi.franchise.api.exception.GlobalExceptionHandler;
import co.com.nequi.franchise.api.validation.RequestValidator;
import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.exception.DuplicateFranchiseException;
import co.com.nequi.franchise.model.exception.InvalidStockException;
import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.TopStockProduct;
import co.com.nequi.franchise.usecase.branch.AddBranchUseCase;
import co.com.nequi.franchise.usecase.branch.UpdateBranchNameUseCase;
import co.com.nequi.franchise.usecase.franchise.CreateFranchiseUseCase;
import co.com.nequi.franchise.usecase.franchise.UpdateFranchiseNameUseCase;
import co.com.nequi.franchise.usecase.product.AddProductUseCase;
import co.com.nequi.franchise.usecase.product.GetTopStockProductsUseCase;
import co.com.nequi.franchise.usecase.product.RemoveProductUseCase;
import co.com.nequi.franchise.usecase.product.UpdateProductNameUseCase;
import co.com.nequi.franchise.usecase.product.UpdateProductStockUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Drives real HTTP over the actual RouterFunction with the use cases mocked: routes, status codes,
 * JSON shape and error translation, without a database or a running server.
 */
@ExtendWith(MockitoExtension.class)
class ApiRouterTest {

    private static final String FRANCHISES = "/api/v1/franchises";
    private static final String FRANCHISE_ID = "f-1";
    private static final String BRANCH_ID = "b-1";
    private static final String PRODUCT_ID = "p-1";

    @Mock
    private CreateFranchiseUseCase createFranchiseUseCase;
    @Mock
    private UpdateFranchiseNameUseCase updateFranchiseNameUseCase;
    @Mock
    private AddBranchUseCase addBranchUseCase;
    @Mock
    private UpdateBranchNameUseCase updateBranchNameUseCase;
    @Mock
    private AddProductUseCase addProductUseCase;
    @Mock
    private RemoveProductUseCase removeProductUseCase;
    @Mock
    private UpdateProductStockUseCase updateProductStockUseCase;
    @Mock
    private UpdateProductNameUseCase updateProductNameUseCase;
    @Mock
    private GetTopStockProductsUseCase getTopStockProductsUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        RequestValidator validator = new RequestValidator(
                Validation.buildDefaultValidatorFactory().getValidator());

        FranchiseHandler franchiseHandler =
                new FranchiseHandler(createFranchiseUseCase, updateFranchiseNameUseCase, validator);
        BranchHandler branchHandler =
                new BranchHandler(addBranchUseCase, updateBranchNameUseCase, validator);
        ProductHandler productHandler = new ProductHandler(addProductUseCase, removeProductUseCase,
                updateProductStockUseCase, updateProductNameUseCase, getTopStockProductsUseCase, validator);

        client = WebTestClient
                .bindToRouterFunction(new ApiRouter().apiRoutes(franchiseHandler, branchHandler, productHandler))
                // bindToRouterFunction builds a minimal pipeline: without registering the handler
                // explicitly, errors would surface as raw 500s and the assertions below would be
                // testing behaviour production does not have.
                .handlerStrategies(HandlerStrategies.builder()
                        .exceptionHandler(new GlobalExceptionHandler(new ObjectMapper()))
                        .build())
                .build();
    }

    @Test
    void createsFranchiseAndReturns201() {
        when(createFranchiseUseCase.execute("Nequi Foods")).thenReturn(Mono.just(
                Franchise.builder().id(FRANCHISE_ID).name("Nequi Foods").build()));

        client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Nequi Foods"}""")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/v1/franchises/f-1")
                .expectBody()
                .jsonPath("$.id").isEqualTo(FRANCHISE_ID)
                .jsonPath("$.name").isEqualTo("Nequi Foods");
    }

    @Test
    void rejectsBlankFranchiseNameWith400() {
        client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "  "}""")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must not be blank"))
                .jsonPath("$.path").isEqualTo(FRANCHISES);
    }

    @Test
    void translatesDuplicateFranchiseInto409() {
        when(createFranchiseUseCase.execute(anyString()))
                .thenReturn(Mono.error(new DuplicateFranchiseException("Nequi Foods")));

        client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Nequi Foods"}""")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409);
    }

    @Test
    void translatesMissingFranchiseInto404() {
        when(updateFranchiseNameUseCase.execute(anyString(), anyString()))
                .thenReturn(Mono.error(ResourceNotFoundException.franchise(FRANCHISE_ID)));

        client.patch().uri(FRANCHISES + "/" + FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Renamed"}""")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString(FRANCHISE_ID));
    }

    @Test
    void addsBranchAndReturns201() {
        when(addBranchUseCase.execute(FRANCHISE_ID, "Centro")).thenReturn(Mono.just(
                Branch.builder().id(BRANCH_ID).name("Centro").franchiseId(FRANCHISE_ID).build()));

        client.post().uri(FRANCHISES + "/" + FRANCHISE_ID + "/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Centro"}""")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(BRANCH_ID)
                .jsonPath("$.franchiseId").isEqualTo(FRANCHISE_ID);
    }

    @Test
    void renamesBranchAndReturns200() {
        when(updateBranchNameUseCase.execute(FRANCHISE_ID, BRANCH_ID, "Poblado")).thenReturn(Mono.just(
                Branch.builder().id(BRANCH_ID).name("Poblado").franchiseId(FRANCHISE_ID).build()));

        client.patch().uri(FRANCHISES + "/" + FRANCHISE_ID + "/branches/" + BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Poblado"}""")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Poblado");
    }

    @Test
    void addsProductAndReturns201() {
        when(addProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, "Espresso", 40)).thenReturn(Mono.just(
                Product.builder().id(PRODUCT_ID).name("Espresso").stock(40).branchId(BRANCH_ID).build()));

        client.post().uri(productsUri())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Espresso", "stock": 40}""")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.stock").isEqualTo(40);
    }

    // Rejected by @Min(0) on the DTO, before the use case is reached. The domain rule is tested
    // separately; this only proves the API answers 400 and not 500.
    @Test
    void rejectsNegativeStockOnCreationWith400() {
        client.post().uri(productsUri())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Espresso", "stock": -3}""")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("zero or positive"));
    }

    @Test
    void translatesDomainStockRuleInto400() {
        when(updateProductStockUseCase.execute(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Mono.error(new InvalidStockException(-1)));

        client.patch().uri(productsUri() + "/" + PRODUCT_ID + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"stock": 5}""")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void updatesStockAndReturns200() {
        when(updateProductStockUseCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 120)).thenReturn(Mono.just(
                Product.builder().id(PRODUCT_ID).name("Espresso").stock(120).branchId(BRANCH_ID).build()));

        client.patch().uri(productsUri() + "/" + PRODUCT_ID + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"stock": 120}""")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.stock").isEqualTo(120);
    }

    @Test
    void renamesProductAndReturns200() {
        when(updateProductNameUseCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Ristretto")).thenReturn(Mono.just(
                Product.builder().id(PRODUCT_ID).name("Ristretto").stock(40).branchId(BRANCH_ID).build()));

        client.patch().uri(productsUri() + "/" + PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Ristretto"}""")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Ristretto");
    }

    @Test
    void removesProductAndReturns204() {
        when(removeProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.empty());

        client.delete().uri(productsUri() + "/" + PRODUCT_ID)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void returnsTopStockProductPerBranch() {
        when(getTopStockProductsUseCase.execute(FRANCHISE_ID)).thenReturn(Flux.just(
                TopStockProduct.builder().branchId("b-1").branchName("Centro")
                        .productId("p-1").productName("Espresso").stock(90).build(),
                TopStockProduct.builder().branchId("b-2").branchName("Poblado")
                        .productId("p-9").productName("Latte").stock(150).build()));

        client.get().uri(FRANCHISES + "/" + FRANCHISE_ID + "/top-stock-products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].branchName").isEqualTo("Centro")
                .jsonPath("$[1].stock").isEqualTo(150);
    }

    // The error arrives from the Flux before any row is emitted. This is why the handler collects
    // the list: streaming it would have committed a 200 status line first.
    @Test
    void returns404OnTopStockWhenFranchiseIsUnknown() {
        when(getTopStockProductsUseCase.execute(FRANCHISE_ID))
                .thenReturn(Flux.error(ResourceNotFoundException.franchise(FRANCHISE_ID)));

        client.get().uri(FRANCHISES + "/" + FRANCHISE_ID + "/top-stock-products")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void rejectsMalformedJsonWith400() {
        client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": ")
                .exchange()
                .expectStatus().isBadRequest();
    }

    private String productsUri() {
        return FRANCHISES + "/" + FRANCHISE_ID + "/branches/" + BRANCH_ID + "/products";
    }
}
