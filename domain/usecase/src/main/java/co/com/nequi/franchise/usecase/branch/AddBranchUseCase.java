package co.com.nequi.franchise.usecase.branch;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.branch.gateways.BranchRepository;
import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class AddBranchUseCase {

    private final FranchiseRepository franchiseRepository;
    private final BranchRepository branchRepository;

    public Mono<Branch> execute(String franchiseId, String name) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(ResourceNotFoundException.franchise(franchiseId)))
                .map(franchise -> Branch.builder()
                        .id(UUID.randomUUID().toString())
                        .name(name)
                        .franchiseId(franchise.getId())
                        .build())
                .flatMap(branchRepository::create);
    }
}
