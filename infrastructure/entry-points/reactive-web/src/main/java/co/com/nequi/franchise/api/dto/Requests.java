package co.com.nequi.franchise.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class Requests {

    private Requests() {
    }

    @Schema(name = "CreateFranchiseRequest", example = "{\"name\": \"Nequi Foods\"}")
    public record CreateFranchiseRequest(
            @NotBlank(message = "must not be blank")
            @Size(max = 120, message = "must be at most 120 characters")
            String name) {
    }

    @Schema(name = "CreateBranchRequest", example = "{\"name\": \"Branch Medellin Centro\"}")
    public record CreateBranchRequest(
            @NotBlank(message = "must not be blank")
            @Size(max = 120, message = "must be at most 120 characters")
            String name) {
    }

    @Schema(name = "CreateProductRequest", example = "{\"name\": \"Espresso\", \"stock\": 40}")
    public record CreateProductRequest(
            @NotBlank(message = "must not be blank")
            @Size(max = 120, message = "must be at most 120 characters")
            String name,

            @NotNull(message = "must not be null")
            @Min(value = 0, message = "must be zero or positive")
            Integer stock) {
    }

    @Schema(name = "UpdateNameRequest", example = "{\"name\": \"New name\"}")
    public record UpdateNameRequest(
            @NotBlank(message = "must not be blank")
            @Size(max = 120, message = "must be at most 120 characters")
            String name) {
    }

    @Schema(name = "UpdateStockRequest", example = "{\"stock\": 120}")
    public record UpdateStockRequest(
            @NotNull(message = "must not be null")
            @Min(value = 0, message = "must be zero or positive")
            Integer stock) {
    }
}
