package co.com.nequi.franchise.model.product;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TopStockProduct {

    String branchId;
    String branchName;
    String productId;
    String productName;
    Integer stock;
}
