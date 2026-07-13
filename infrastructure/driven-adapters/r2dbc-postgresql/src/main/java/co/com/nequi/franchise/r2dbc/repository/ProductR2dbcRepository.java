package co.com.nequi.franchise.r2dbc.repository;

import co.com.nequi.franchise.r2dbc.entity.ProductEntity;
import co.com.nequi.franchise.r2dbc.entity.TopStockRow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductR2dbcRepository extends ReactiveCrudRepository<ProductEntity, String> {

    /**
     * DISTINCT ON (b.id) keeps the first row of each branch group; ORDER BY stock DESC makes that
     * the highest-stock product. Ties broken by product id so the result is deterministic.
     */
    @Query("""
            SELECT DISTINCT ON (b.id)
                   b.id   AS branch_id,
                   b.name AS branch_name,
                   p.id   AS product_id,
                   p.name AS product_name,
                   p.stock AS stock
            FROM branch b
            JOIN product p ON p.branch_id = b.id
            WHERE b.franchise_id = :franchiseId
            ORDER BY b.id, p.stock DESC, p.id ASC
            """)
    Flux<TopStockRow> findTopStockByFranchiseId(String franchiseId);
}
