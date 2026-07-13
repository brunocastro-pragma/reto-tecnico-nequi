package co.com.nequi.franchise.usecase.franchise;

import co.com.nequi.franchise.model.exception.DuplicateFranchiseException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private CreateFranchiseUseCase useCase;

    @Test
    void createsFranchiseWithGeneratedIdWhenNameIsFree() {
        when(franchiseRepository.existsByName("Nequi Foods")).thenReturn(Mono.just(false));
        when(franchiseRepository.create(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute("Nequi Foods"))
                .assertNext(franchise -> {
                    assertThat(franchise.getName()).isEqualTo("Nequi Foods");
                    assertThat(franchise.getId()).isNotBlank();
                })
                .verifyComplete();
    }

    @Test
    void rejectsDuplicateNameAndDoesNotPersist() {
        when(franchiseRepository.existsByName("Nequi Foods")).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute("Nequi Foods"))
                .expectError(DuplicateFranchiseException.class)
                .verify();

        verify(franchiseRepository, never()).create(any());
    }
}
