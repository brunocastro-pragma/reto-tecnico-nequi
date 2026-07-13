package co.com.nequi.franchise.r2dbc.adapter;

import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.TopStockProduct;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import co.com.nequi.franchise.r2dbc.mapper.EntityMapper;
import co.com.nequi.franchise.r2dbc.repository.ProductR2dbcRepository;
import co.com.nequi.franchise.r2dbc.resilience.ResilienceDecorator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductR2dbcRepository repository;
    private final R2dbcEntityTemplate template;
    private final ResilienceDecorator resilience;

    @Override
    public Mono<Product> create(Product product) {
        return resilience.decorate(
                template.insert(EntityMapper.toEntity(product))
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Product> update(Product product) {
        return resilience.decorate(
                template.update(EntityMapper.toEntity(product))
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Product> findById(String id) {
        return resilience.decorate(
                repository.findById(id)
                        .map(EntityMapper::toDomain));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return resilience.decorate(repository.deleteById(id));
    }

    @Override
    public Flux<TopStockProduct> findTopStockByFranchiseId(String franchiseId) {
        return resilience.decorate(
                repository.findTopStockByFranchiseId(franchiseId)
                        .map(EntityMapper::toDomain));
    }
}
