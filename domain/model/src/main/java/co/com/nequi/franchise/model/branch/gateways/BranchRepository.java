package co.com.nequi.franchise.model.branch.gateways;

import co.com.nequi.franchise.model.branch.Branch;
import reactor.core.publisher.Mono;

public interface BranchRepository {

    Mono<Branch> create(Branch branch);

    Mono<Branch> update(Branch branch);

    Mono<Branch> findById(String id);
}
