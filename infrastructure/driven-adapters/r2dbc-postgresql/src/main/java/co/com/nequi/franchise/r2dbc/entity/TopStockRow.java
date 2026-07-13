package co.com.nequi.franchise.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;

/** Projection of the top-stock query. Maps no table, only that result set. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopStockRow {

    @Column("branch_id")
    private String branchId;

    @Column("branch_name")
    private String branchName;

    @Column("product_id")
    private String productId;

    @Column("product_name")
    private String productName;

    @Column("stock")
    private Integer stock;
}
