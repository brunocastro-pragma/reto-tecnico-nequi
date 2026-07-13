package co.com.nequi.franchise.r2dbc.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The thresholds here are deliberately tiny (window of 2, no wait between retries) so the
 * behaviour is observable in milliseconds. Production values live in application.yml.
 */
class ResilienceDecoratorTest {

    private static final String INSTANCE = "postgres";

    private ResilienceDecorator decorator;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry circuitBreakers = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());

        RetryRegistry retries = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .retryExceptions(DataAccessResourceFailureException.class, TimeoutException.class)
                .build());

        TimeLimiterRegistry timeLimiters = TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(100))
                .build());

        decorator = new ResilienceDecorator(circuitBreakers, retries, timeLimiters);
        circuitBreaker = circuitBreakers.circuitBreaker(INSTANCE);
    }

    @Test
    void passesSuccessfulCallsThroughUntouched() {
        StepVerifier.create(decorator.decorate(Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();
    }

    // maxAttempts counts the original call, so three attempts, not four.
    @Test
    void retriesTransientFailuresUpToTheConfiguredNumberOfAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> alwaysFailing = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new DataAccessResourceFailureException("connection reset"));
        });

        StepVerifier.create(decorator.decorate(alwaysFailing))
                .expectError(DataAccessResourceFailureException.class)
                .verify();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void succeedsWhenARetryRecoversFromATransientFailure() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> failsOnce = Mono.defer(() -> attempts.incrementAndGet() == 1
                ? Mono.error(new DataAccessResourceFailureException("connection reset"))
                : Mono.just("recovered"));

        StepVerifier.create(decorator.decorate(failsOnce))
                .expectNext("recovered")
                .verifyComplete();

        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void failsSlowCallsWithATimeout() {
        Mono<String> tooSlow = Mono.just("late").delayElement(Duration.ofSeconds(2));

        StepVerifier.create(decorator.decorate(tooSlow))
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(5));
    }

    // Once open, the breaker fails fast with CallNotPermittedException instead of queueing
    // requests against a database that is already down. The API turns that into a 503.
    @Test
    void opensTheCircuitAfterRepeatedFailuresAndRejectsFurtherCalls() {
        Mono<String> failing = Mono.error(new DataAccessResourceFailureException("db is down"));

        // Two failed calls fill the window (each one after exhausting its retries).
        StepVerifier.create(decorator.decorate(failing))
                .expectError(DataAccessResourceFailureException.class)
                .verify();
        StepVerifier.create(decorator.decorate(failing))
                .expectError(DataAccessResourceFailureException.class)
                .verify();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // The third call never reaches the database.
        StepVerifier.create(decorator.decorate(Mono.just("would have worked")))
                .expectError(CallNotPermittedException.class)
                .verify();
    }
}
