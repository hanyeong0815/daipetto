package koh.portfolio.springapi.application.healthrecord.service;

import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.HealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.UpdateHealthRecordRequest;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.healthrecord.exception.HealthRecordErrorCode;
import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import koh.portfolio.springapi.domain.healthrecord.port.HealthRecordRepository;
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
import static org.mockito.Mockito.*;

class HealthRecordServiceTest {

    private HealthRecordRepository healthRecordRepository;
    private PetRepository petRepository;
    private HealthRecordService healthRecordService;
    private UpdateHealthRecordService updateHealthRecordService;
    private DeleteHealthRecordService deleteHealthRecordService;

    private final Long userId = 1L;
    private final Long petId = 10L;
    private final Long healthRecordId = 100L;

    @BeforeEach
    void setUp() {
        healthRecordRepository = mock(HealthRecordRepository.class);
        petRepository = mock(PetRepository.class);
        healthRecordService = new HealthRecordService(healthRecordRepository, petRepository);
        updateHealthRecordService = new UpdateHealthRecordService(healthRecordRepository, petRepository);
        deleteHealthRecordService = new DeleteHealthRecordService(healthRecordRepository, petRepository);
    }

    private Pet pet(Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return new Pet(petId, ownerId, "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE,
                new BigDecimal("4.50"), now, now, null);
    }

    private HealthRecord healthRecord() {
        LocalDateTime now = LocalDateTime.now();
        return new HealthRecord(healthRecordId, petId, new BigDecimal("4.70"), "咳", "少し元気がない",
                LocalDate.of(2026, 5, 20), now, now, null);
    }

    private void stubSaveEcho() {
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(invocation -> {
            HealthRecord arg = invocation.getArgument(0);
            return new HealthRecord(arg.getId() != null ? arg.getId() : healthRecordId, arg.getPetId(), arg.getWeight(),
                    arg.getSymptom(), arg.getMemo(), arg.getRecordedDate(), arg.getCreatedAt(), arg.getUpdatedAt(), arg.getDeletedAt());
        });
    }

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("HEALTH-T001: 健康記録登録成功時、healthRecordIdを返却する")
    void create_health_record_success() {
        // given
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(
                new BigDecimal("4.70"), "咳, 食欲低下", "少し元気がない", LocalDate.of(2026, 5, 20));

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        stubSaveEcho();

        // when
        CreateHealthRecordResponse response = healthRecordService.execute(userId, petId, request);

        // then
        assertThat(response.healthRecordId()).isEqualTo(healthRecordId);

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getPetId()).isEqualTo(petId);
        assertThat(captor.getValue().getRecordedDate()).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    @DisplayName("recordedDate省略時、当日の日付で登録する")
    void create_health_record_defaults_recorded_date_to_today() {
        // given
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(new BigDecimal("4.70"), null, null, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        stubSaveEcho();

        // when
        healthRecordService.execute(userId, petId, request);

        // then
        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getRecordedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("HEALTH-T002: 他ユーザーのペットへの登録時、HEALTH-002例外が発生する")
    void create_health_record_fail_when_not_pet_owner() {
        // given
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(new BigDecimal("4.70"), null, null, null);

        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertThatThrownBy(() -> healthRecordService.execute(userId, petId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HealthRecordErrorCode.NOT_PET_OWNER.code()));

        verify(healthRecordRepository, never()).save(any(HealthRecord.class));
    }

    @Test
    @DisplayName("存在しないペットへの登録時、HEALTH-002例外が発生する")
    void create_health_record_fail_when_pet_not_found() {
        // given
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(new BigDecimal("4.70"), null, null, null);

        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> healthRecordService.execute(userId, petId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HealthRecordErrorCode.NOT_PET_OWNER.code()));
    }

    // ---------------------------------------------------------------- getList

    @Test
    @DisplayName("HEALTH-T004: 健康記録一覧取得成功時、登録済データを返却する")
    void get_health_record_list_success() {
        // given
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        when(healthRecordRepository.findAllByPetId(petId)).thenReturn(List.of(healthRecord()));

        // when
        List<HealthRecordResponse> result = healthRecordService.execute(userId, petId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).healthRecordId()).isEqualTo(healthRecordId);
        assertThat(result.get(0).weight()).isEqualByComparingTo("4.70");
        assertThat(result.get(0).symptom()).isEqualTo("咳");
        assertThat(result.get(0).recordedDate()).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    @DisplayName("他ユーザーのペットの一覧取得時、HEALTH-002例外が発生する")
    void get_health_record_list_fail_when_not_pet_owner() {
        // given
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertThatThrownBy(() -> healthRecordService.execute(userId, petId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HealthRecordErrorCode.NOT_PET_OWNER.code()));

        verify(healthRecordRepository, never()).findAllByPetId(anyLong());
    }

    // ----------------------------------------------------------------- update

    @Test
    @DisplayName("健康記録更新成功時、変更内容が保存される")
    void update_health_record_success() {
        // given
        UpdateHealthRecordRequest request = new UpdateHealthRecordRequest(
                new BigDecimal("5.10"), "食欲回復", "元気になった", LocalDate.of(2026, 5, 25));

        when(healthRecordRepository.findById(healthRecordId)).thenReturn(Optional.of(healthRecord()));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        stubSaveEcho();

        // when
        updateHealthRecordService.execute(userId, healthRecordId, request);

        // then
        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(healthRecordId);
        assertThat(captor.getValue().getWeight()).isEqualByComparingTo("5.10");
        assertThat(captor.getValue().getSymptom()).isEqualTo("食欲回復");
        assertThat(captor.getValue().getRecordedDate()).isEqualTo(LocalDate.of(2026, 5, 25));
    }

    @Test
    @DisplayName("recordedDate省略時の更新では、既存の記録日を維持する")
    void update_health_record_keeps_recorded_date_when_omitted() {
        // given
        UpdateHealthRecordRequest request = new UpdateHealthRecordRequest(new BigDecimal("5.10"), null, null, null);

        when(healthRecordRepository.findById(healthRecordId)).thenReturn(Optional.of(healthRecord()));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        stubSaveEcho();

        // when
        updateHealthRecordService.execute(userId, healthRecordId, request);

        // then
        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getRecordedDate()).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    @DisplayName("存在しない健康記録の更新時、HEALTH-001例外が発生する")
    void update_health_record_fail_when_not_found() {
        // given
        UpdateHealthRecordRequest request = new UpdateHealthRecordRequest(new BigDecimal("5.10"), null, null, null);

        when(healthRecordRepository.findById(healthRecordId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateHealthRecordService.execute(userId, healthRecordId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HealthRecordErrorCode.HEALTH_RECORD_NOT_FOUND.code()));

        verify(healthRecordRepository, never()).save(any(HealthRecord.class));
    }

    @Test
    @DisplayName("他ユーザーのペットの記録更新時、HEALTH-002例外が発生する")
    void update_health_record_fail_when_not_pet_owner() {
        // given
        UpdateHealthRecordRequest request = new UpdateHealthRecordRequest(new BigDecimal("5.10"), null, null, null);

        when(healthRecordRepository.findById(healthRecordId)).thenReturn(Optional.of(healthRecord()));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertThatThrownBy(() -> updateHealthRecordService.execute(userId, healthRecordId, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HealthRecordErrorCode.NOT_PET_OWNER.code()));

        verify(healthRecordRepository, never()).save(any(HealthRecord.class));
    }

    // ----------------------------------------------------------------- delete

    @Test
    @DisplayName("HEALTH-T005: 健康記録削除時、deleted_atを設定する")
    void delete_health_record_success() {
        // given
        when(healthRecordRepository.findById(healthRecordId)).thenReturn(Optional.of(healthRecord()));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));

        // when
        deleteHealthRecordService.execute(userId, healthRecordId);

        // then
        verify(healthRecordRepository).softDelete(eq(healthRecordId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("他ユーザーのペットの記録削除時、HEALTH-002例外が発生する")
    void delete_health_record_fail_when_not_pet_owner() {
        // given
        when(healthRecordRepository.findById(healthRecordId)).thenReturn(Optional.of(healthRecord()));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertThatThrownBy(() -> deleteHealthRecordService.execute(userId, healthRecordId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(HealthRecordErrorCode.NOT_PET_OWNER.code()));

        verify(healthRecordRepository, never()).softDelete(anyLong(), any(LocalDateTime.class));
    }
}
