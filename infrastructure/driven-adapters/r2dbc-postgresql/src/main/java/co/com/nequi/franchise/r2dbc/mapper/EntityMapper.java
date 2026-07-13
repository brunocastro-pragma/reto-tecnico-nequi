package co.com.nequi.franchise.r2dbc.mapper;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.TopStockProduct;
import co.com.nequi.franchise.r2dbc.entity.BranchEntity;
import co.com.nequi.franchise.r2dbc.entity.FranchiseEntity;
import co.com.nequi.franchise.r2dbc.entity.ProductEntity;
import co.com.nequi.franchise.r2dbc.entity.TopStockRow;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static FranchiseEntity toEntity(Franchise franchise) {
        return FranchiseEntity.builder()
                .id(franchise.getId())
                .name(franchise.getName())
                .build();
    }

    public static Franchise toDomain(FranchiseEntity entity) {
        return Franchise.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public static BranchEntity toEntity(Branch branch) {
        return BranchEntity.builder()
                .id(branch.getId())
                .name(branch.getName())
                .franchiseId(branch.getFranchiseId())
                .build();
    }

    public static Branch toDomain(BranchEntity entity) {
        return Branch.builder()
                .id(entity.getId())
                .name(entity.getName())
                .franchiseId(entity.getFranchiseId())
                .build();
    }

    public static ProductEntity toEntity(Product product) {
        return ProductEntity.builder()
                .id(product.getId())
                .name(product.getName())
                .stock(product.getStock())
                .branchId(product.getBranchId())
                .build();
    }

    public static Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .stock(entity.getStock())
                .branchId(entity.getBranchId())
                .build();
    }

    public static TopStockProduct toDomain(TopStockRow row) {
        return TopStockProduct.builder()
                .branchId(row.getBranchId())
                .branchName(row.getBranchName())
                .productId(row.getProductId())
                .productName(row.getProductName())
                .stock(row.getStock())
                .build();
    }
}
