package co.com.nequi.franchise.api.validation;

import co.com.nequi.franchise.api.exception.InvalidRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * With RouterFunctions there is no @Valid, so the body is validated explicitly. The result is a
 * Mono carrying either the valid body or an error signal -- one more step of the chain.
 */
@Component
@RequiredArgsConstructor
public class RequestValidator {

    private final Validator validator;

    public <T> Mono<T> validate(T body) {
        return Flux.fromIterable(validator.validate(body))
                .map(RequestValidator::describe)
                .collectList()
                .flatMap(violations -> Mono.just(violations)
                        .filter(List::isEmpty)
                        .map(empty -> body)
                        .switchIfEmpty(Mono.error(
                                new InvalidRequestException(String.join("; ", violations)))));
    }

    private static <T> String describe(ConstraintViolation<T> violation) {
        return "%s %s".formatted(violation.getPropertyPath(), violation.getMessage());
    }
}
