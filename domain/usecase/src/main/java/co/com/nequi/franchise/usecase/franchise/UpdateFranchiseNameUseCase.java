package co.com.nequi.franchise.usecase.franchise;

import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UpdateFranchiseNameUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> execute(String franchiseId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(ResourceNotFoundException.franchise(franchiseId)))
                .map(franchise -> franchise.toBuilder().name(newName).build())
                .flatMap(franchiseRepository::update);
    }
}
