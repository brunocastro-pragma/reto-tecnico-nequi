package co.com.nequi.franchise.r2dbc.adapter;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.branch.gateways.BranchRepository;
import co.com.nequi.franchise.r2dbc.mapper.EntityMapper;
import co.com.nequi.franchise.r2dbc.repository.BranchR2dbcRepository;
import co.com.nequi.franchise.r2dbc.resilience.ResilienceDecorator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class BranchRepositoryAdapter implements BranchRepository {

    private final BranchR2dbcRepository repository;
    private final R2dbcEntityTemplate template;
    private final ResilienceDecorator resilience;

    @Override
    public Mono<Branch> create(Branch branch) {
        return resilience.decorate(
                template.insert(EntityMapper.toEntity(branch))
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Branch> update(Branch branch) {
        return resilience.decorate(
                template.update(EntityMapper.toEntity(branch))
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Branch> findById(String id) {
        return resilience.decorate(
                repository.findById(id)
                        .map(EntityMapper::toDomain));
    }
}
