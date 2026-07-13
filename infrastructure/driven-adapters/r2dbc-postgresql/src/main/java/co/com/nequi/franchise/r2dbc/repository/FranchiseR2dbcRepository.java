package co.com.nequi.franchise.r2dbc.repository;

import co.com.nequi.franchise.r2dbc.entity.FranchiseEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface FranchiseR2dbcRepository extends ReactiveCrudRepository<FranchiseEntity, String> {

    Mono<Boolean> existsByName(String name);
}
