package koh.portfolio.springapi.application.pet.service;

import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetRequest;
import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetResponse;
import koh.portfolio.springapi.application.pet.dto.PetDto.PetDetailResponse;
import koh.portfolio.springapi.application.pet.dto.PetDto.PetSummary;
import koh.portfolio.springapi.application.pet.dto.PetDto.UpdatePetRequest;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.pet.exception.PetErrorCode;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PetServiceTest {

    private PetRepository petRepository;
    private PetService petService;
    private GetPetDetailService getPetDetailService;
    private UpdatePetService updatePetService;
    private DeletePetService deletePetService;

    @BeforeEach
    void setUp() {
        petRepository = mock(PetRepository.class);
        petService = new PetService(petRepository);
        getPetDetailService = new GetPetDetailService(petRepository);
        updatePetService = new UpdatePetService(petRepository);
        deletePetService = new DeletePetService(petRepository);
    }

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("ペット登録成功時、登録されたpetIdを返却する")
    void create_pet_success() {
        // given
        Long userId = 1L;
        CreatePetRequest request = new CreatePetRequest(
                "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5")
        );

        LocalDateTime now = LocalDateTime.now();
        Pet savedPet = new Pet(1L, userId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null);

        when(petRepository.save(any(Pet.class))).thenReturn(savedPet);

        // when
        CreatePetResponse response = petService.execute(userId, request);

        // then
        assertThat(response.petId()).isEqualTo(1L);
        verify(petRepository).save(any(Pet.class));
    }

    // ---------------------------------------------------------------- getList

    @Test
    @DisplayName("ペット一覧取得成功時、ユーザーのペット一覧を返却する")
    void get_pet_list_success() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        List<Pet> pets = List.of(
                new Pet(1L, userId, "Momo", PetType.CAT,
                        LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null),
                new Pet(2L, userId, "Kuma", PetType.DOG,
                        LocalDate.of(2022, 5, 10), PetGender.MALE, new BigDecimal("8.0"), now, now, null)
        );

        when(petRepository.findAllByUserId(userId)).thenReturn(pets);

        // when
        List<PetSummary> result = petService.execute(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Momo");
        assertThat(result.get(1).name()).isEqualTo("Kuma");
        verify(petRepository).findAllByUserId(userId);
    }

    // --------------------------------------------------------------- getDetail

    @Test
    @DisplayName("ペット詳細取得成功時、ペット情報を返却する")
    void get_pet_detail_success() {
        // given
        Long userId = 1L;
        Long petId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Pet pet = new Pet(petId, userId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        // when
        PetDetailResponse response = getPetDetailService.execute(userId, petId);

        // then
        assertThat(response.id()).isEqualTo(petId);
        assertThat(response.name()).isEqualTo("Momo");
        assertThat(response.petType()).isEqualTo(PetType.CAT);
        assertThat(response.gender()).isEqualTo(PetGender.FEMALE);
    }

    @Test
    @DisplayName("存在しないペットの詳細取得時、PET-001例外が発生する")
    void get_pet_detail_fail_when_not_found() {
        // given
        Long userId = 1L;
        Long petId = 999L;

        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getPetDetailService.execute(userId, petId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(PetErrorCode.PET_NOT_FOUND.code());
                });
    }

    @Test
    @DisplayName("他人のペット詳細取得時、PET-002例外が発生する")
    void get_pet_detail_fail_when_not_owner() {
        // given
        Long userId = 1L;
        Long anotherUserId = 2L;
        Long petId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Pet pet = new Pet(petId, anotherUserId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        // when & then
        assertThatThrownBy(() -> getPetDetailService.execute(userId, petId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(PetErrorCode.NOT_PET_OWNER.code());
                });
    }

    // ----------------------------------------------------------------- update

    @Test
    @DisplayName("ペット情報更新成功時、petRepository.saveが呼ばれる")
    void update_pet_success() {
        // given
        Long userId = 1L;
        Long petId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Pet pet = new Pet(petId, userId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null);

        UpdatePetRequest request = new UpdatePetRequest(
                "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("5.0")
        );

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petRepository.save(any(Pet.class))).thenReturn(pet);

        // when
        updatePetService.execute(userId, petId, request);

        // then
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    @DisplayName("存在しないペット更新時、PET-001例外が発生する")
    void update_pet_fail_when_not_found() {
        // given
        Long userId = 1L;
        Long petId = 999L;

        UpdatePetRequest request = new UpdatePetRequest(
                "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("5.0")
        );

        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updatePetService.execute(userId, petId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(PetErrorCode.PET_NOT_FOUND.code());
                });

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    @DisplayName("他人のペット更新時、PET-002例外が発生する")
    void update_pet_fail_when_not_owner() {
        // given
        Long userId = 1L;
        Long anotherUserId = 2L;
        Long petId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Pet pet = new Pet(petId, anotherUserId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null);

        UpdatePetRequest request = new UpdatePetRequest(
                "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("5.0")
        );

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        // when & then
        assertThatThrownBy(() -> updatePetService.execute(userId, petId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(PetErrorCode.NOT_PET_OWNER.code());
                });

        verify(petRepository, never()).save(any(Pet.class));
    }

    // ----------------------------------------------------------------- delete

    @Test
    @DisplayName("ペット論理削除成功時、petRepository.softDeleteが呼ばれる")
    void delete_pet_success() {
        // given
        Long userId = 1L;
        Long petId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Pet pet = new Pet(petId, userId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        // when
        deletePetService.execute(userId, petId);

        // then
        verify(petRepository).softDelete(eq(petId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("存在しないペット削除時、PET-001例外が発生する")
    void delete_pet_fail_when_not_found() {
        // given
        Long userId = 1L;
        Long petId = 999L;

        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deletePetService.execute(userId, petId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(PetErrorCode.PET_NOT_FOUND.code());
                });

        verify(petRepository, never()).softDelete(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("他人のペット削除時、PET-002例外が発生する")
    void delete_pet_fail_when_not_owner() {
        // given
        Long userId = 1L;
        Long anotherUserId = 2L;
        Long petId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Pet pet = new Pet(petId, anotherUserId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"), now, now, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        // when & then
        assertThatThrownBy(() -> deletePetService.execute(userId, petId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getErrorCode().code()).isEqualTo(PetErrorCode.NOT_PET_OWNER.code());
                });

        verify(petRepository, never()).softDelete(anyLong(), any(LocalDateTime.class));
    }
}
