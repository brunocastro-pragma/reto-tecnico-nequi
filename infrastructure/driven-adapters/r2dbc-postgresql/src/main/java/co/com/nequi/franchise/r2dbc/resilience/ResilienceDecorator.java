package co.com.nequi.franchise.r2dbc.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resilience for every call to the database, composed as reactive operators.
 *
 * Order matters: TimeLimiter innermost bounds each attempt, Retry wraps it with backoff, and the
 * CircuitBreaker sits outermost so it records the outcome of the call rather than of each attempt.
 */
@Component
public class ResilienceDecorator {

    private static final String INSTANCE = "postgres";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public ResilienceDecorator(CircuitBreakerRegistry circuitBreakerRegistry,
                               RetryRegistry retryRegistry,
                               TimeLimiterRegistry timeLimiterRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.retry = retryRegistry.retry(INSTANCE);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(INSTANCE);
    }

    public <T> Mono<T> decorate(Mono<T> source) {
        return source
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

    public <T> Flux<T> decorate(Flux<T> source) {
        return source
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }
}
