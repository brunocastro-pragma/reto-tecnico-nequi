package co.com.nequi.franchise.r2dbc.repository;

import co.com.nequi.franchise.r2dbc.entity.BranchEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface BranchR2dbcRepository extends ReactiveCrudRepository<BranchEntity, String> {
}
