package co.com.nequi.franchise.api;

import co.com.nequi.franchise.api.dto.Requests.CreateBranchRequest;
import co.com.nequi.franchise.api.dto.Requests.UpdateNameRequest;
import co.com.nequi.franchise.api.mapper.DtoMapper;
import co.com.nequi.franchise.api.validation.RequestValidator;
import co.com.nequi.franchise.usecase.branch.AddBranchUseCase;
import co.com.nequi.franchise.usecase.branch.UpdateBranchNameUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchHandler {

    private final AddBranchUseCase addBranchUseCase;
    private final UpdateBranchNameUseCase updateBranchNameUseCase;
    private final RequestValidator validator;

    public Mono<ServerResponse> add(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        return request.bodyToMono(CreateBranchRequest.class)
                .flatMap(validator::validate)
                .flatMap(body -> addBranchUseCase.execute(franchiseId, body.name()))
                .map(DtoMapper::toResponse)
                .flatMap(response -> ServerResponse.created(
                                request.uriBuilder().path("/{id}").build(response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> updateName(ServerRequest request) {
        String franchiseId = request.pathVariable("franchiseId");
        String branchId = request.pathVariable("branchId");
        return request.bodyToMono(UpdateNameRequest.class)
                .flatMap(validator::validate)
                .flatMap(body -> updateBranchNameUseCase.execute(franchiseId, branchId, body.name()))
                .map(DtoMapper::toResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }
}
