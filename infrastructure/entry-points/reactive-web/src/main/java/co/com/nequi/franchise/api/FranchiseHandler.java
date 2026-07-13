package co.com.nequi.franchise.api;

import co.com.nequi.franchise.api.dto.Requests.CreateFranchiseRequest;
import co.com.nequi.franchise.api.dto.Requests.UpdateNameRequest;
import co.com.nequi.franchise.api.mapper.DtoMapper;
import co.com.nequi.franchise.api.validation.RequestValidator;
import co.com.nequi.franchise.usecase.franchise.CreateFranchiseUseCase;
import co.com.nequi.franchise.usecase.franchise.UpdateFranchiseNameUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Orchestration only: read the body, validate it, call the use case, shape the response.
 * Errors travel as error signals up to GlobalExceptionHandler.
 */
@Component
@RequiredArgsConstructor
public class FranchiseHandler {

    private final CreateFranchiseUseCase createFranchiseUseCase;
    private final UpdateFranchiseNameUseCase updateFranchiseNameUseCase;
    private final RequestValidator validator;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CreateFranchiseRequest.class)
                .flatMap(validator::validate)
                .flatMap(body -> createFranchiseUseCase.execute(body.name()))
                .map(DtoMapper::toResponse)
                .flatMap(response -> ServerResponse.created(
                                request.uriBuilder().path("/{id}").build(response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> updateName(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        return request.bodyToMono(UpdateNameRequest.class)
                .flatMap(validator::validate)
                .flatMap(body -> updateFranchiseNameUseCase.execute(franchiseId, body.name()))
                .map(DtoMapper::toResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }
}
