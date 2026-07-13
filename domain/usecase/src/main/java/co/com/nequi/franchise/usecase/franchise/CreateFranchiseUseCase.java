package co.com.nequi.franchise.usecase.franchise;

import co.com.nequi.franchise.model.exception.DuplicateFranchiseException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> execute(String name) {
        return franchiseRepository.existsByName(name)
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new DuplicateFranchiseException(name)))
                .map(exists -> Franchise.builder()
                        .id(UUID.randomUUID().toString())
                        .name(name)
                        .build())
                .flatMap(franchiseRepository::create);
    }
}
