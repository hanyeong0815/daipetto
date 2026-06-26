package koh.portfolio.springapi.presentation.pet;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.pet.dto.PetDto;
import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetResponse;
import koh.portfolio.springapi.application.pet.dto.PetDto.PetDetailResponse;
import koh.portfolio.springapi.application.pet.dto.PetDto.PetSummary;
import koh.portfolio.springapi.application.pet.usecase.CreatePetUseCase;
import koh.portfolio.springapi.application.pet.usecase.DeletePetUseCase;
import koh.portfolio.springapi.application.pet.usecase.GetPetDetailUseCase;
import koh.portfolio.springapi.application.pet.usecase.GetPetListUseCase;
import koh.portfolio.springapi.application.pet.usecase.UpdatePetUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {
    private final CreatePetUseCase createPetUseCase;
    private final GetPetListUseCase getPetListUseCase;
    private final GetPetDetailUseCase getPetDetailUseCase;
    private final UpdatePetUseCase updatePetUseCase;
    private final DeletePetUseCase deletePetUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePetResponse>> createPet(
            Authentication authentication,
            @Valid @RequestBody PetDto.CreatePetRequest request
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createPetUseCase.execute(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PetSummary>>> getPetList(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getPetListUseCase.execute(userId)));
    }

    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetDetailResponse>> getPetDetail(
            Authentication authentication,
            @PathVariable Long petId
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getPetDetailUseCase.execute(userId, petId)));
    }

    @PatchMapping("/{petId}")
    public ResponseEntity<ApiResponse<Void>> updatePet(
            Authentication authentication,
            @PathVariable Long petId,
            @Valid @RequestBody PetDto.UpdatePetRequest request
    ) {
        Long userId = extractUserId(authentication);
        updatePetUseCase.execute(userId, petId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<ApiResponse<Void>> deletePet(
            Authentication authentication,
            @PathVariable Long petId
    ) {
        Long userId = extractUserId(authentication);
        deletePetUseCase.execute(userId, petId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
