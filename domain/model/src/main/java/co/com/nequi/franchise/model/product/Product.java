package co.com.nequi.franchise.model.product;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Product {

    String id;
    String name;
    Integer stock;
    String branchId;
}
