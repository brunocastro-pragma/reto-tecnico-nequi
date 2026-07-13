package co.com.nequi.franchise.api;

import co.com.nequi.franchise.api.dto.Requests.CreateProductRequest;
import co.com.nequi.franchise.api.dto.Requests.UpdateNameRequest;
import co.com.nequi.franchise.api.dto.Requests.UpdateStockRequest;
import co.com.nequi.franchise.api.mapper.DtoMapper;
import co.com.nequi.franchise.api.validation.RequestValidator;
import co.com.nequi.franchise.usecase.product.AddProductUseCase;
import co.com.nequi.franchise.usecase.product.GetTopStockProductsUseCase;
import co.com.nequi.franchise.usecase.product.RemoveProductUseCase;
import co.com.nequi.franchise.usecase.product.UpdateProductNameUseCase;
import co.com.nequi.franchise.usecase.product.UpdateProductStockUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductHandler {

    private final AddProductUseCase addProductUseCase;
    private final RemoveProductUseCase removeProductUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final UpdateProductNameUseCase updateProductNameUseCase;
    private final GetTopStockProductsUseCase getTopStockProductsUseCase;
    private final RequestValidator validator;

    public Mono<ServerResponse> add(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        return request.bodyToMono(CreateProductRequest.class)
                .flatMap(validator::validate)
                .flatMap(body -> addProductUseCase.execute(franchiseId, branchId, body.name(), body.stock()))
                .map(DtoMapper::toResponse)
                .flatMap(response -> ServerResponse.created(
                                request.uriBuilder().path("/{id}").build(response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> remove(ServerRequest request) {
        return removeProductUseCase.execute(
                        request.pathVariable("franchiseId"),
                        request.pathVariable("branchId"),
                        request.pathVariable("productId"))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> updateStock(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");
        return request.bodyToMono(UpdateStockRequest.class)
                .flatMap(validator::validate)
                .flatMap(body -> updateProductStockUseCase.execute(franchiseId, branchId, productId, body.stock()))
                .map(DtoMapper::toResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> updateName(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        String productId = request.pathVariable("productId");
        return request.bodyToMono(UpdateNameRequest.class)
                .flatMap(validator::validate)
                .flatMap(body -> updateProductNameUseCase.execute(franchiseId, branchId, productId, body.name()))
                .map(DtoMapper::toResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    /**
     * collectList, not ServerResponse.body(flux): streaming the Flux commits the status line
     * before the first row is known, and the "franchise does not exist" error then arrives too
     * late to become a 404. The result is bounded -- one row per branch -- so collecting is cheap.
     */
    public Mono<ServerResponse> topStock(ServerRequest request) {
        return getTopStockProductsUseCase.execute(request.pathVariable("franchiseId"))
                .map(DtoMapper::toResponse)
                .collectList()
                .flatMap(products -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(products));
    }
}
