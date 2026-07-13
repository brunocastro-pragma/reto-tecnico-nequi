package co.com.nequi.franchise.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product")
public class ProductEntity {

    @Id
    private String id;
    private String name;
    private Integer stock;

    @Column("branch_id")
    private String branchId;
}
