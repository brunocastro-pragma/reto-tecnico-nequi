package co.com.nequi.franchise.api.mapper;

import co.com.nequi.franchise.api.dto.Responses.BranchResponse;
import co.com.nequi.franchise.api.dto.Responses.FranchiseResponse;
import co.com.nequi.franchise.api.dto.Responses.ProductResponse;
import co.com.nequi.franchise.api.dto.Responses.TopStockProductResponse;
import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.product.Product;
import co.com.nequi.franchise.model.product.TopStockProduct;
import reactor.core.publisher.Flux;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static FranchiseResponse toResponse(Franchise franchise) {
        return new FranchiseResponse(franchise.getId(), franchise.getName());
    }

    public static BranchResponse toResponse(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getName(), branch.getFranchiseId());
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getStock(), product.getBranchId());
    }

    public static TopStockProductResponse toResponse(TopStockProduct topStockProduct) {
        return new TopStockProductResponse(
                topStockProduct.getBranchId(),
                topStockProduct.getBranchName(),
                topStockProduct.getProductId(),
                topStockProduct.getProductName(),
                topStockProduct.getStock());
    }
}
