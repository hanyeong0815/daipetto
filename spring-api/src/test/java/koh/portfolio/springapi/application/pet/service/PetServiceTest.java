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
import org.mockito.ArgumentCaptor;

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
                "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345"
        );

        LocalDateTime now = LocalDateTime.now();
        Pet savedPet = new Pet(1L, userId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345", now, now, null);

        when(petRepository.save(any(Pet.class))).thenReturn(savedPet);

        // when
        CreatePetResponse response = petService.execute(userId, request);

        // then
        assertThat(response.petId()).isEqualTo(1L);
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    @DisplayName("ペット登録成功時、breed・neutered・microchipNumberがPetに正しく渡される")
    void create_pet_passes_new_fields_to_domain() {
        // given
        Long userId = 1L;
        CreatePetRequest request = new CreatePetRequest(
                "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345"
        );

        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        petService.execute(userId, request);

        // then
        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(captor.capture());
        Pet captured = captor.getValue();
        assertThat(captured.getBreed()).isEqualTo("Scottish Fold");
        assertThat(captured.isNeutered()).isTrue();
        assertThat(captured.getMicrochipNumber()).isEqualTo("123456789012345");
    }

    @Test
    @DisplayName("ペット登録時、breedとmicrochipNumberはnullでも登録できる")
    void create_pet_success_when_breed_and_microchip_null() {
        // given
        Long userId = 1L;
        CreatePetRequest request = new CreatePetRequest(
                "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                null, false, null
        );

        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CreatePetResponse response = petService.execute(userId, request);

        // then
        assertThat(response).isNotNull();
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
                        LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                        "Scottish Fold", true, "123456789012345", now, now, null),
                new Pet(2L, userId, "Kuma", PetType.DOG,
                        LocalDate.of(2022, 5, 10), PetGender.MALE, new BigDecimal("8.0"),
                        "Shiba", false, null, now, now, null)
        );

        when(petRepository.findAllByUserId(userId)).thenReturn(pets);

        // when
        List<PetSummary> result = petService.execute(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Momo");
        assertThat(result.get(0).breed()).isEqualTo("Scottish Fold");
        assertThat(result.get(1).name()).isEqualTo("Kuma");
        assertThat(result.get(1).breed()).isEqualTo("Shiba");
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
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345", now, now, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        // when
        PetDetailResponse response = getPetDetailService.execute(userId, petId);

        // then
        assertThat(response.id()).isEqualTo(petId);
        assertThat(response.name()).isEqualTo("Momo");
        assertThat(response.petType()).isEqualTo(PetType.CAT);
        assertThat(response.gender()).isEqualTo(PetGender.FEMALE);
        assertThat(response.breed()).isEqualTo("Scottish Fold");
        assertThat(response.neutered()).isTrue();
        assertThat(response.microchipNumber()).isEqualTo("123456789012345");
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
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345", now, now, null);

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
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", false, null, now, now, null);

        UpdatePetRequest request = new UpdatePetRequest(
                "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("5.0"),
                "Scottish Fold", true, "123456789012345"
        );

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petRepository.save(any(Pet.class))).thenReturn(pet);

        // when
        updatePetService.execute(userId, petId, request);

        // then
        verify(petRepository).save(any(Pet.class));
    }

    @Test
    @DisplayName("ペット情報更新時、breed・neutered・microchipNumberが更新される")
    void update_pet_applies_new_fields() {
        // given
        Long userId = 1L;
        Long petId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Pet pet = new Pet(petId, userId, "Momo", PetType.CAT,
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", false, null, now, now, null);

        UpdatePetRequest request = new UpdatePetRequest(
                "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Mix", true, "123456789012345"
        );

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        updatePetService.execute(userId, petId, request);

        // then
        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(captor.capture());
        Pet updated = captor.getValue();
        assertThat(updated.getBreed()).isEqualTo("Mix");
        assertThat(updated.isNeutered()).isTrue();
        assertThat(updated.getMicrochipNumber()).isEqualTo("123456789012345");
    }

    @Test
    @DisplayName("存在しないペット更新時、PET-001例外が発生する")
    void update_pet_fail_when_not_found() {
        // given
        Long userId = 1L;
        Long petId = 999L;

        UpdatePetRequest request = new UpdatePetRequest(
                "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("5.0"),
                "Scottish Fold", true, "123456789012345"
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
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", false, null, now, now, null);

        UpdatePetRequest request = new UpdatePetRequest(
                "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("5.0"),
                "Scottish Fold", true, "123456789012345"
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
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345", now, now, null);

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
                LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345", now, now, null);

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
