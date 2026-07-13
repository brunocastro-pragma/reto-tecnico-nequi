package co.com.nequi.franchise.usecase.franchise;

import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateFranchiseNameUseCaseTest {

    private static final String FRANCHISE_ID = "f-1";

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private UpdateFranchiseNameUseCase useCase;

    @Test
    void renamesFranchiseKeepingItsId() {
        Franchise existing = Franchise.builder().id(FRANCHISE_ID).name("Old").build();
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(existing));
        when(franchiseRepository.update(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, "New"))
                .expectNextMatches(franchise -> franchise.getId().equals(FRANCHISE_ID)
                        && franchise.getName().equals("New"))
                .verifyComplete();
    }

    @Test
    void failsWhenFranchiseDoesNotExist() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(FRANCHISE_ID, "New"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(franchiseRepository, never()).update(any());
    }
}
