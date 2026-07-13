package co.com.nequi.franchise.api;

import co.com.nequi.franchise.api.dto.Requests.CreateBranchRequest;
import co.com.nequi.franchise.api.dto.Requests.CreateFranchiseRequest;
import co.com.nequi.franchise.api.dto.Requests.CreateProductRequest;
import co.com.nequi.franchise.api.dto.Requests.UpdateNameRequest;
import co.com.nequi.franchise.api.dto.Requests.UpdateStockRequest;
import co.com.nequi.franchise.api.dto.Responses.BranchResponse;
import co.com.nequi.franchise.api.dto.Responses.ErrorResponse;
import co.com.nequi.franchise.api.dto.Responses.FranchiseResponse;
import co.com.nequi.franchise.api.dto.Responses.ProductResponse;
import co.com.nequi.franchise.api.dto.Responses.TopStockProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * The nine endpoints. With functional routing springdoc has no annotated methods to introspect,
 * so each route declares its OpenAPI contract through @RouterOperation.
 */
@Configuration
public class ApiRouter {

    private static final String FRANCHISES = "/api/v1/franchises";
    private static final String FRANCHISE = FRANCHISES + "/{franchiseId}";
    private static final String BRANCHES = FRANCHISE + "/branches";
    private static final String BRANCH = BRANCHES + "/{branchId}";
    private static final String PRODUCTS = BRANCH + "/products";
    private static final String PRODUCT = PRODUCTS + "/{productId}";
    private static final String TOP_STOCK = FRANCHISE + "/top-stock-products";

    private static final String TAG_FRANCHISE = "Franchises";
    private static final String TAG_BRANCH = "Branches";
    private static final String TAG_PRODUCT = "Products";

    @Bean
    @RouterOperations({
            @RouterOperation(path = FRANCHISES, method = org.springframework.web.bind.annotation.RequestMethod.POST,
                    beanClass = FranchiseHandler.class, beanMethod = "create",
                    operation = @Operation(operationId = "createFranchise", tags = TAG_FRANCHISE,
                            summary = "Create a franchise",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = CreateFranchiseRequest.class))),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                                            description = "Franchise created",
                                            content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                                            description = "Blank or too long name",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                                            description = "A franchise with that name already exists",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = FRANCHISE, method = org.springframework.web.bind.annotation.RequestMethod.PATCH,
                    beanClass = FranchiseHandler.class, beanMethod = "updateName",
                    operation = @Operation(operationId = "updateFranchiseName", tags = TAG_FRANCHISE,
                            summary = "Update the name of a franchise",
                            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "franchiseId",
                                    in = ParameterIn.PATH, required = true),
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = UpdateNameRequest.class))),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                                            description = "Franchise updated",
                                            content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Franchise not found",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = BRANCHES, method = org.springframework.web.bind.annotation.RequestMethod.POST,
                    beanClass = BranchHandler.class, beanMethod = "add",
                    operation = @Operation(operationId = "addBranch", tags = TAG_BRANCH,
                            summary = "Add a branch to a franchise",
                            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "franchiseId",
                                    in = ParameterIn.PATH, required = true),
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = CreateBranchRequest.class))),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                                            description = "Branch created",
                                            content = @Content(schema = @Schema(implementation = BranchResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Franchise not found",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = BRANCH, method = org.springframework.web.bind.annotation.RequestMethod.PATCH,
                    beanClass = BranchHandler.class, beanMethod = "updateName",
                    operation = @Operation(operationId = "updateBranchName", tags = TAG_BRANCH,
                            summary = "Update the name of a branch",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = UpdateNameRequest.class))),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                                            description = "Branch updated",
                                            content = @Content(schema = @Schema(implementation = BranchResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Branch not found in that franchise",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = PRODUCTS, method = org.springframework.web.bind.annotation.RequestMethod.POST,
                    beanClass = ProductHandler.class, beanMethod = "add",
                    operation = @Operation(operationId = "addProduct", tags = TAG_PRODUCT,
                            summary = "Add a product to a branch",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = CreateProductRequest.class))),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                                            description = "Product created",
                                            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                                            description = "Negative stock",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Branch not found in that franchise",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = PRODUCT, method = org.springframework.web.bind.annotation.RequestMethod.DELETE,
                    beanClass = ProductHandler.class, beanMethod = "remove",
                    operation = @Operation(operationId = "removeProduct", tags = TAG_PRODUCT,
                            summary = "Remove a product from a branch",
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204",
                                            description = "Product removed"),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Product not found in that branch",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = PRODUCT + "/stock", method = org.springframework.web.bind.annotation.RequestMethod.PATCH,
                    beanClass = ProductHandler.class, beanMethod = "updateStock",
                    operation = @Operation(operationId = "updateProductStock", tags = TAG_PRODUCT,
                            summary = "Update the stock of a product",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = UpdateStockRequest.class))),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                                            description = "Stock updated",
                                            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                                            description = "Negative stock",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Product not found in that branch",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = PRODUCT, method = org.springframework.web.bind.annotation.RequestMethod.PATCH,
                    beanClass = ProductHandler.class, beanMethod = "updateName",
                    operation = @Operation(operationId = "updateProductName", tags = TAG_PRODUCT,
                            summary = "Update the name of a product",
                            requestBody = @RequestBody(required = true,
                                    content = @Content(schema = @Schema(implementation = UpdateNameRequest.class))),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                                            description = "Product updated",
                                            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Product not found in that branch",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            })),

            @RouterOperation(path = TOP_STOCK, method = org.springframework.web.bind.annotation.RequestMethod.GET,
                    beanClass = ProductHandler.class, beanMethod = "topStock",
                    operation = @Operation(operationId = "getTopStockProducts", tags = TAG_PRODUCT,
                            summary = "Get the product with the highest stock of each branch of a franchise",
                            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "franchiseId",
                                    in = ParameterIn.PATH, required = true),
                            responses = {
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                                            description = "One entry per branch",
                                            content = @Content(array = @ArraySchema(
                                                    schema = @Schema(implementation = TopStockProductResponse.class)))),
                                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                                            description = "Franchise not found",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }))
    })
    public RouterFunction<ServerResponse> apiRoutes(FranchiseHandler franchiseHandler,
                                                    BranchHandler branchHandler,
                                                    ProductHandler productHandler) {
        return route()
                .POST(FRANCHISES, accept(MediaType.APPLICATION_JSON), franchiseHandler::create)
                .PATCH(FRANCHISE, accept(MediaType.APPLICATION_JSON), franchiseHandler::updateName)

                .POST(BRANCHES, accept(MediaType.APPLICATION_JSON), branchHandler::add)
                .PATCH(BRANCH, accept(MediaType.APPLICATION_JSON), branchHandler::updateName)

                .POST(PRODUCTS, accept(MediaType.APPLICATION_JSON), productHandler::add)
                .DELETE(PRODUCT, productHandler::remove)
                // Routes are evaluated in declaration order, so the more specific one goes first.
                .PATCH(PRODUCT + "/stock", accept(MediaType.APPLICATION_JSON), productHandler::updateStock)
                .PATCH(PRODUCT, accept(MediaType.APPLICATION_JSON), productHandler::updateName)

                .GET(TOP_STOCK, productHandler::topStock)
                .build();
    }
}
