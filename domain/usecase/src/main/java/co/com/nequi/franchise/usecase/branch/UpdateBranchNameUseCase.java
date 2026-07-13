package co.com.nequi.franchise.usecase.branch;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.branch.gateways.BranchRepository;
import co.com.nequi.franchise.usecase.shared.OwnershipResolver;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UpdateBranchNameUseCase {

    private final OwnershipResolver ownershipResolver;
    private final BranchRepository branchRepository;

    public Mono<Branch> execute(String franchiseId, String branchId, String newName) {
        return ownershipResolver.branchOfFranchise(franchiseId, branchId)
                .map(branch -> branch.toBuilder().name(newName).build())
                .flatMap(branchRepository::update);
    }
}
