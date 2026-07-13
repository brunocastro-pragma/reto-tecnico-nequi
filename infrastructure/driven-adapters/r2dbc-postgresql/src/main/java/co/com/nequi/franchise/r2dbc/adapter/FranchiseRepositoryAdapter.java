package co.com.nequi.franchise.r2dbc.adapter;

import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import co.com.nequi.franchise.r2dbc.mapper.EntityMapper;
import co.com.nequi.franchise.r2dbc.repository.FranchiseR2dbcRepository;
import co.com.nequi.franchise.r2dbc.resilience.ResilienceDecorator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * insert() and update() are explicit instead of a single save(): R2DBC has no persistence context,
 * so save() decides by looking at the id, and ids are assigned in the domain -- every insert would
 * be taken for an update and affect zero rows.
 */
@Repository
@RequiredArgsConstructor
public class FranchiseRepositoryAdapter implements FranchiseRepository {

    private final FranchiseR2dbcRepository repository;
    private final R2dbcEntityTemplate template;
    private final ResilienceDecorator resilience;

    @Override
    public Mono<Franchise> create(Franchise franchise) {
        return resilience.decorate(
                template.insert(EntityMapper.toEntity(franchise))
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Franchise> update(Franchise franchise) {
        return resilience.decorate(
                template.update(EntityMapper.toEntity(franchise))
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Franchise> findById(String id) {
        return resilience.decorate(
                repository.findById(id)
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return resilience.decorate(repository.existsByName(name));
    }
}
