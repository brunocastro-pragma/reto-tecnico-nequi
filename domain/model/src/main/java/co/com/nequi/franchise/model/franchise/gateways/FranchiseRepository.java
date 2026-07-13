package co.com.nequi.franchise.model.franchise.gateways;

import co.com.nequi.franchise.model.franchise.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepository {

    Mono<Franchise> create(Franchise franchise);

    Mono<Franchise> update(Franchise franchise);

    Mono<Franchise> findById(String id);

    Mono<Boolean> existsByName(String name);
}
