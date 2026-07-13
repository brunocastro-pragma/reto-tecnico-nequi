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
@Table("branch")
public class BranchEntity {

    @Id
    private String id;
    private String name;

    @Column("franchise_id")
    private String franchiseId;
}
