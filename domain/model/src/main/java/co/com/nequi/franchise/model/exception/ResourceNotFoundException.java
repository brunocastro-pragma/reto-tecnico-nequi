package co.com.nequi.franchise.model.exception;

public final class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, String id) {
        super("%s with id [%s] was not found".formatted(resource, id));
    }

    public static ResourceNotFoundException franchise(String id) {
        return new ResourceNotFoundException("Franchise", id);
    }

    // Also used when the branch exists but hangs off another franchise: it does not exist
    // under the requested one, and 404 avoids leaking resources the caller cannot reach.
    public static ResourceNotFoundException branch(String id) {
        return new ResourceNotFoundException("Branch", id);
    }

    public static ResourceNotFoundException product(String id) {
        return new ResourceNotFoundException("Product", id);
    }
}
