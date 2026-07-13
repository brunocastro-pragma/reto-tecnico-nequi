package co.com.nequi.franchise.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class Responses {

    private Responses() {
    }

    @Schema(name = "FranchiseResponse")
    public record FranchiseResponse(String id, String name) {
    }

    @Schema(name = "BranchResponse")
    public record BranchResponse(String id, String name, String franchiseId) {
    }

    @Schema(name = "ProductResponse")
    public record ProductResponse(String id, String name, Integer stock, String branchId) {
    }

    @Schema(name = "TopStockProductResponse",
            description = "The product with the highest stock of a branch")
    public record TopStockProductResponse(String branchId,
                                          String branchName,
                                          String productId,
                                          String productName,
                                          Integer stock) {
    }

    @Schema(name = "ErrorResponse")
    public record ErrorResponse(String timestamp,
                                int status,
                                String error,
                                String message,
                                String path) {
    }
}
