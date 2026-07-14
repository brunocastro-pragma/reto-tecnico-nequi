package co.com.nequi.franchise.api.exception;

import co.com.nequi.franchise.model.exception.DuplicateFranchiseException;
import co.com.nequi.franchise.model.exception.InvalidStockException;
import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every business failure has exactly one status, and this is where that contract is pinned down.
 * The router test proves the wiring; this proves the table.
 */
class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(objectMapper);

    @Test
    void notFoundBecomes404() {
        assertStatus(ResourceNotFoundException.franchise("f-1"), HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidStockBecomes400() {
        assertStatus(new InvalidStockException(-1), HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicateFranchiseBecomes409() {
        assertStatus(new DuplicateFranchiseException("Nequi Foods"), HttpStatus.CONFLICT);
    }

    @Test
    void invalidRequestBecomes400() {
        assertStatus(new InvalidRequestException("name must not be blank"), HttpStatus.BAD_REQUEST);
    }

    /**
     * An open circuit means the database is down, not that the request was wrong: the caller gets
     * a 503, the status that tells a client it is worth trying again later.
     */
    @Test
    void openCircuitBecomes503() {
        assertStatus(CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.ofDefaults("postgres")), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void timeoutBecomes504() {
        assertStatus(new TimeoutException("db did not answer"), HttpStatus.GATEWAY_TIMEOUT);
    }

    /**
     * The bug this test exists for: Spring raises ResponseStatusException for a path no route
     * matches. It was not in the table, so it fell through to the 500 branch -- and "that endpoint
     * does not exist" reached the client as "the service is broken".
     */
    @Test
    void unknownRouteKeepsSpringsOwnStatusInsteadOfBecoming500() {
        assertStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching handler"),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void unmappedErrorBecomes500AndHidesItsMessage() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handleAndReadBody(exchange, new IllegalStateException("jdbc://user:pass@host")))
                .assertNext(body -> {
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    // The exception message must never reach the client: it can carry a stack
                    // trace, a SQL fragment or a connection string.
                    assertThat(body.get("message").asText())
                            .isEqualTo("Unexpected error. Please contact support.")
                            .doesNotContain("user:pass");
                })
                .verifyComplete();
    }

    @Test
    void theBodyCarriesTheRequestPathAndAReadableMessage() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handleAndReadBody(exchange, ResourceNotFoundException.branch("b-9")))
                .assertNext(body -> {
                    assertThat(body.get("status").asInt()).isEqualTo(404);
                    assertThat(body.get("error").asText()).isEqualTo("Not Found");
                    assertThat(body.get("message").asText()).contains("b-9");
                    assertThat(body.get("path").asText()).isEqualTo("/api/v1/franchises");
                    assertThat(body.get("timestamp").asText()).isNotBlank();
                })
                .verifyComplete();
    }

    private void assertStatus(Throwable error, HttpStatus expected) {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(handleAndReadBody(exchange, error))
                .assertNext(body -> {
                    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(expected);
                    assertThat(body.get("status").asInt()).isEqualTo(expected.value());
                })
                .verifyComplete();
    }

    /**
     * The handler writes the body into the response, so reading it back is another step of the
     * same chain -- not a .block() on the side. fromCallable turns readTree's checked exception
     * into an error signal, which is what keeps this free of a try/catch.
     */
    private Mono<JsonNode> handleAndReadBody(MockServerWebExchange exchange, Throwable error) {
        return handler.handle(exchange, error)
                // Mono.defer, not getBodyAsString() directly: without it the body is read at
                // assembly time -- before the handler has written anything into the response.
                .then(Mono.defer(() -> exchange.getResponse().getBodyAsString()))
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readTree(json)));
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/franchises").build());
    }
}
