package co.com.nequi.franchise.api.exception;

import co.com.nequi.franchise.api.dto.Responses.ErrorResponse;
import co.com.nequi.franchise.model.exception.DuplicateFranchiseException;
import co.com.nequi.franchise.model.exception.InvalidStockException;
import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Single place where errors become HTTP responses. @Order(-2) puts it ahead of Spring's
 * DefaultErrorWebExceptionHandler, which sits at -1 and would answer first with a generic 500.
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements WebExceptionHandler {

    // LinkedHashMap: lookup walks it in order and takes the first assignable type, so the most
    // specific entries must come first.
    private static final Map<Class<? extends Throwable>, HttpStatus> STATUS_BY_EXCEPTION = new LinkedHashMap<>();

    static {
        STATUS_BY_EXCEPTION.put(ResourceNotFoundException.class, HttpStatus.NOT_FOUND);
        STATUS_BY_EXCEPTION.put(InvalidStockException.class, HttpStatus.BAD_REQUEST);
        STATUS_BY_EXCEPTION.put(DuplicateFranchiseException.class, HttpStatus.CONFLICT);
        STATUS_BY_EXCEPTION.put(InvalidRequestException.class, HttpStatus.BAD_REQUEST);
        // Raised by bodyToMono when the JSON is malformed or a field has the wrong type.
        STATUS_BY_EXCEPTION.put(ServerWebInputException.class, HttpStatus.BAD_REQUEST);
        STATUS_BY_EXCEPTION.put(CallNotPermittedException.class, HttpStatus.SERVICE_UNAVAILABLE);
        STATUS_BY_EXCEPTION.put(TimeoutException.class, HttpStatus.GATEWAY_TIMEOUT);
    }

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        HttpStatus status = resolveStatus(error);

        return Mono.just(status)
                .filter(HttpStatus.INTERNAL_SERVER_ERROR::equals)
                .doOnNext(unknown -> log.error("Unhandled failure on {}",
                        exchange.getRequest().getPath(), error))
                .then(writeResponse(exchange, status, error));
    }

    // isAssignableFrom, not an exact class match: the exception that arrives is often a subtype.
    private HttpStatus resolveStatus(Throwable error) {
        for (Map.Entry<Class<? extends Throwable>, HttpStatus> entry : STATUS_BY_EXCEPTION.entrySet()) {
            if (entry.getKey().isAssignableFrom(error.getClass())) {
                return entry.getValue();
            }
        }
        // Spring raises these for anything it can answer on its own -- an unknown path, an
        // unsupported method. They already carry the right status; reporting them as 500 would
        // turn "that route does not exist" into "the service is broken".
        return error instanceof ResponseStatusException statusException
                ? HttpStatus.valueOf(statusException.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, Throwable error) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                messageOf(status, error),
                exchange.getRequest().getPath().value());

        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(body))
                .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes))
                .flatMap(buffer -> exchange.getResponse().writeWith(Mono.just(buffer)));
    }

    // A 500 is an unknown failure: its message could leak a SQL fragment or a connection string.
    private String messageOf(HttpStatus status, Throwable error) {
        return HttpStatus.INTERNAL_SERVER_ERROR.equals(status)
                ? "Unexpected error. Please contact support."
                : error.getMessage();
    }
}
