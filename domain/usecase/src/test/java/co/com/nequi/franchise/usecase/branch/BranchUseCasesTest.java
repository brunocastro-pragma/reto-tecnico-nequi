package co.com.nequi.franchise.usecase.branch;

import co.com.nequi.franchise.model.branch.Branch;
import co.com.nequi.franchise.model.branch.gateways.BranchRepository;
import co.com.nequi.franchise.model.exception.ResourceNotFoundException;
import co.com.nequi.franchise.model.franchise.Franchise;
import co.com.nequi.franchise.model.franchise.gateways.FranchiseRepository;
import co.com.nequi.franchise.model.product.gateways.ProductRepository;
import co.com.nequi.franchise.usecase.shared.OwnershipResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class BranchUseCasesTest {

    private static final String FRANCHISE_ID = "f-1";
    private static final String BRANCH_ID = "b-1";

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    void addsBranchUnderAnExistingFranchise() {
        AddBranchUseCase useCase = new AddBranchUseCase(franchiseRepository, branchRepository);
        when(franchiseRepository.findById(FRANCHISE_ID))
                .thenReturn(Mono.just(Franchise.builder().id(FRANCHISE_ID).name("Nequi Foods").build()));
        when(branchRepository.create(any(Branch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, "Medellin Centro"))
                .assertNext(branch -> {
                    assertThat(branch.getId()).isNotBlank();
                    assertThat(branch.getName()).isEqualTo("Medellin Centro");
                    assertThat(branch.getFranchiseId()).isEqualTo(FRANCHISE_ID);
                })
                .verifyComplete();
    }

    @Test
    void refusesToAddBranchToAFranchiseThatDoesNotExist() {
        AddBranchUseCase useCase = new AddBranchUseCase(franchiseRepository, branchRepository);
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(FRANCHISE_ID, "Medellin Centro"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(branchRepository, never()).create(any());
    }

    @Test
    void renamesBranchThatBelongsToTheFranchise() {
        UpdateBranchNameUseCase useCase = new UpdateBranchNameUseCase(ownershipResolver(), branchRepository);
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Mono.just(
                Branch.builder().id(BRANCH_ID).name("Old").franchiseId(FRANCHISE_ID).build()));
        when(branchRepository.update(any(Branch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "New"))
                .expectNextMatches(branch -> branch.getName().equals("New"))
                .verifyComplete();
    }

    // The branch exists but hangs off another franchise. Without the ownership check a caller
    // could rename any branch in the system by guessing its id.
    @Test
    void refusesToRenameBranchOfAnotherFranchise() {
        UpdateBranchNameUseCase useCase = new UpdateBranchNameUseCase(ownershipResolver(), branchRepository);
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Mono.just(
                Branch.builder().id(BRANCH_ID).name("Old").franchiseId("another-franchise").build()));

        StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "New"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(branchRepository, never()).update(any());
    }

    private OwnershipResolver ownershipResolver() {
        return new OwnershipResolver(branchRepository, productRepository);
    }
}
