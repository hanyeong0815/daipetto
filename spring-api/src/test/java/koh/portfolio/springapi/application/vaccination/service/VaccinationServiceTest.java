package koh.portfolio.springapi.application.vaccination.service;

import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationRequest;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationResponse;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.VaccinationResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.notification.model.Notification;
import koh.portfolio.springapi.domain.notification.model.NotificationType;
import koh.portfolio.springapi.domain.notification.port.NotificationRepository;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.vaccination.exception.VaccinationErrorCode;
import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationReminderTarget;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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

class VaccinationServiceTest {

    private VaccinationRepository vaccinationRepository;
    private PetRepository petRepository;
    private NotificationRepository notificationRepository;
    private VaccinationService vaccinationService;
    private CompleteVaccinationService completeVaccinationService;
    private RescheduleVaccinationService rescheduleVaccinationService;
    private CancelVaccinationService cancelVaccinationService;
    private DeleteVaccinationService deleteVaccinationService;
    private SendVaccinationRemindersService sendVaccinationRemindersService;

    private final Long userId = 1L;
    private final Long petId = 10L;
    private final Long vaccinationId = 100L;

    @BeforeEach
    void setUp() {
        vaccinationRepository = mock(VaccinationRepository.class);
        petRepository = mock(PetRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        vaccinationService = new VaccinationService(vaccinationRepository, petRepository);
        completeVaccinationService = new CompleteVaccinationService(vaccinationRepository, petRepository);
        rescheduleVaccinationService = new RescheduleVaccinationService(vaccinationRepository, petRepository);
        cancelVaccinationService = new CancelVaccinationService(vaccinationRepository, petRepository);
        deleteVaccinationService = new DeleteVaccinationService(vaccinationRepository, petRepository);
        sendVaccinationRemindersService = new SendVaccinationRemindersService(vaccinationRepository, notificationRepository);
    }

    private Pet pet(Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return new Pet(petId, ownerId, "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE,
                new BigDecimal("4.50"), null, false, null, now, now, null);
    }

    private Vaccination vaccination(VaccinationStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new Vaccination(vaccinationId, petId, "混合ワクチン", LocalDate.of(2026, 10, 10),
                status == VaccinationStatus.COMPLETED ? LocalDate.of(2026, 10, 10) : null, null,
                status, now, now, null);
    }

    private void givenOwnedVaccination(VaccinationStatus status) {
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.of(vaccination(status)));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
    }

    private void assertErrorCode(ThrowingCallable call, VaccinationErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code()).isEqualTo(errorCode.code()));
    }

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("接種予定登録成功時、SCHEDULEDで保存しvaccinationIdを返却する")
    void create_vaccination_success() {
        // given
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        when(vaccinationRepository.save(any(Vaccination.class))).thenAnswer(invocation -> {
            Vaccination arg = invocation.getArgument(0);
            return new Vaccination(vaccinationId, arg.getPetId(), arg.getVaccineName(), arg.getScheduledDate(),
                    arg.getCompletedDate(), arg.getRemindedDate(), arg.getStatus(), arg.getCreatedAt(), arg.getUpdatedAt(), arg.getDeletedAt());
        });

        // when
        CreateVaccinationResponse response = vaccinationService.execute(userId, petId,
                new CreateVaccinationRequest("混合ワクチン", LocalDate.of(2026, 10, 10)));

        // then
        assertThat(response.vaccinationId()).isEqualTo(vaccinationId);

        ArgumentCaptor<Vaccination> captor = ArgumentCaptor.forClass(Vaccination.class);
        verify(vaccinationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VaccinationStatus.SCHEDULED);
        assertThat(captor.getValue().getCompletedDate()).isNull();
        assertThat(captor.getValue().getScheduledDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    @DisplayName("他ユーザーのペットへの登録時、VACCINATION-002例外が発生する")
    void create_vaccination_fail_when_not_pet_owner() {
        // given
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertErrorCode(() -> vaccinationService.execute(userId, petId,
                new CreateVaccinationRequest("混合ワクチン", LocalDate.of(2026, 10, 10))),
                VaccinationErrorCode.NOT_PET_OWNER);

        verify(vaccinationRepository, never()).save(any(Vaccination.class));
    }

    // ---------------------------------------------------------------- getList

    @Test
    @DisplayName("接種予定一覧取得成功時、登録済データを返却する")
    void get_vaccination_list_success() {
        // given
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        when(vaccinationRepository.findAllByPetId(petId)).thenReturn(List.of(vaccination(VaccinationStatus.SCHEDULED)));

        // when
        List<VaccinationResponse> result = vaccinationService.execute(userId, petId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).vaccinationId()).isEqualTo(vaccinationId);
        assertThat(result.get(0).vaccineName()).isEqualTo("混合ワクチン");
        assertThat(result.get(0).status()).isEqualTo(VaccinationStatus.SCHEDULED);
    }

    @Test
    @DisplayName("存在しないペットの一覧取得時、VACCINATION-002例外が発生する")
    void get_vaccination_list_fail_when_pet_not_found() {
        // given
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> vaccinationService.execute(userId, petId), VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).findAllByPetId(anyLong());
    }

    // --------------------------------------------------------------- complete

    @Test
    @DisplayName("SCHEDULED→COMPLETED: 指定した接種完了日で遷移元条件付き更新を行う")
    void complete_vaccination_success() {
        // given
        givenOwnedVaccination(VaccinationStatus.SCHEDULED);
        when(vaccinationRepository.markCompleted(eq(vaccinationId), any(), any())).thenReturn(true);

        // when
        completeVaccinationService.execute(userId, vaccinationId, LocalDate.of(2026, 10, 9));

        // then
        verify(vaccinationRepository).markCompleted(eq(vaccinationId), eq(LocalDate.of(2026, 10, 9)), any(LocalDateTime.class));
        verify(vaccinationRepository, never()).save(any(Vaccination.class));
    }

    @Test
    @DisplayName("接種完了日省略時、当日を接種完了日とする")
    void complete_vaccination_defaults_completed_date_to_today() {
        // given
        givenOwnedVaccination(VaccinationStatus.SCHEDULED);
        when(vaccinationRepository.markCompleted(eq(vaccinationId), any(), any())).thenReturn(true);

        // when
        completeVaccinationService.execute(userId, vaccinationId, null);

        // then
        verify(vaccinationRepository).markCompleted(eq(vaccinationId), eq(LocalDate.now()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("COMPLETED・CANCELLEDからの接種完了はVACCINATION-003例外が発生する")
    void complete_vaccination_fail_when_not_scheduled() {
        for (VaccinationStatus status : List.of(VaccinationStatus.COMPLETED, VaccinationStatus.CANCELLED)) {
            // given
            givenOwnedVaccination(status);

            // when & then
            assertErrorCode(() -> completeVaccinationService.execute(userId, vaccinationId, null),
                    VaccinationErrorCode.INVALID_STATE_TRANSITION);
        }
        verify(vaccinationRepository, never()).markCompleted(anyLong(), any(), any());
    }

    @Test
    @DisplayName("読み取り後に別の遷移が確定し更新0件の場合、VACCINATION-003例外が発生する")
    void complete_vaccination_fail_when_transition_lost() {
        // given
        givenOwnedVaccination(VaccinationStatus.SCHEDULED);
        when(vaccinationRepository.markCompleted(eq(vaccinationId), any(), any())).thenReturn(false);

        // when & then
        assertErrorCode(() -> completeVaccinationService.execute(userId, vaccinationId, null),
                VaccinationErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("存在しない接種予定の完了時、VACCINATION-001例外が発生する")
    void complete_vaccination_fail_when_not_found() {
        // given
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> completeVaccinationService.execute(userId, vaccinationId, null),
                VaccinationErrorCode.VACCINATION_NOT_FOUND);
    }

    @Test
    @DisplayName("他ユーザーのペットの接種完了時、VACCINATION-002例外が発生する")
    void complete_vaccination_fail_when_not_pet_owner() {
        // given
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.of(vaccination(VaccinationStatus.SCHEDULED)));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertErrorCode(() -> completeVaccinationService.execute(userId, vaccinationId, null),
                VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).markCompleted(anyLong(), any(), any());
    }

    // ------------------------------------------------------------- reschedule

    @Test
    @DisplayName("CANCELLED→SCHEDULED: 新しい接種予定日で再登録する")
    void reschedule_vaccination_success() {
        // given
        givenOwnedVaccination(VaccinationStatus.CANCELLED);
        when(vaccinationRepository.reschedule(eq(vaccinationId), any(), any())).thenReturn(true);

        // when
        rescheduleVaccinationService.execute(userId, vaccinationId, LocalDate.of(2026, 11, 1));

        // then
        verify(vaccinationRepository).reschedule(eq(vaccinationId), eq(LocalDate.of(2026, 11, 1)), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("SCHEDULED・COMPLETEDからの再登録はVACCINATION-003例外が発生する")
    void reschedule_vaccination_fail_when_not_cancelled() {
        for (VaccinationStatus status : List.of(VaccinationStatus.SCHEDULED, VaccinationStatus.COMPLETED)) {
            // given
            givenOwnedVaccination(status);

            // when & then
            assertErrorCode(() -> rescheduleVaccinationService.execute(userId, vaccinationId, LocalDate.of(2026, 11, 1)),
                    VaccinationErrorCode.INVALID_STATE_TRANSITION);
        }
        verify(vaccinationRepository, never()).reschedule(anyLong(), any(), any());
    }

    @Test
    @DisplayName("他ユーザーのペットの再登録時、VACCINATION-002例外が発生する")
    void reschedule_vaccination_fail_when_not_pet_owner() {
        // given
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.of(vaccination(VaccinationStatus.CANCELLED)));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertErrorCode(() -> rescheduleVaccinationService.execute(userId, vaccinationId, LocalDate.of(2026, 11, 1)),
                VaccinationErrorCode.NOT_PET_OWNER);
    }

    // ----------------------------------------------------------------- cancel

    @Test
    @DisplayName("SCHEDULED→CANCELLED: 遷移元条件付き更新を行う")
    void cancel_vaccination_success() {
        // given
        givenOwnedVaccination(VaccinationStatus.SCHEDULED);
        when(vaccinationRepository.cancel(eq(vaccinationId), any())).thenReturn(true);

        // when
        cancelVaccinationService.execute(userId, vaccinationId);

        // then
        verify(vaccinationRepository).cancel(eq(vaccinationId), any(LocalDateTime.class));
        verify(vaccinationRepository, never()).save(any(Vaccination.class));
    }

    @Test
    @DisplayName("COMPLETED・CANCELLEDからの中止はVACCINATION-003例外が発生する")
    void cancel_vaccination_fail_when_not_scheduled() {
        for (VaccinationStatus status : List.of(VaccinationStatus.COMPLETED, VaccinationStatus.CANCELLED)) {
            // given
            givenOwnedVaccination(status);

            // when & then
            assertErrorCode(() -> cancelVaccinationService.execute(userId, vaccinationId),
                    VaccinationErrorCode.INVALID_STATE_TRANSITION);
        }
        verify(vaccinationRepository, never()).cancel(anyLong(), any());
    }

    @Test
    @DisplayName("読み取り後に別の遷移が確定し更新0件の場合、中止はVACCINATION-003例外が発生する")
    void cancel_vaccination_fail_when_transition_lost() {
        // given
        givenOwnedVaccination(VaccinationStatus.SCHEDULED);
        when(vaccinationRepository.cancel(eq(vaccinationId), any())).thenReturn(false);

        // when & then
        assertErrorCode(() -> cancelVaccinationService.execute(userId, vaccinationId),
                VaccinationErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("存在しない接種予定の中止時、VACCINATION-001例外が発生する")
    void cancel_vaccination_fail_when_not_found() {
        // given
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> cancelVaccinationService.execute(userId, vaccinationId),
                VaccinationErrorCode.VACCINATION_NOT_FOUND);
    }

    @Test
    @DisplayName("他ユーザーのペットの中止時、VACCINATION-002例外が発生する")
    void cancel_vaccination_fail_when_not_pet_owner() {
        // given
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.of(vaccination(VaccinationStatus.SCHEDULED)));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertErrorCode(() -> cancelVaccinationService.execute(userId, vaccinationId),
                VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).cancel(anyLong(), any());
    }

    // ----------------------------------------------------------------- delete

    @Test
    @DisplayName("接種予定削除時、論理削除を行う")
    void delete_vaccination_success() {
        // given
        givenOwnedVaccination(VaccinationStatus.COMPLETED);
        when(vaccinationRepository.softDelete(eq(vaccinationId), any())).thenReturn(true);

        // when
        deleteVaccinationService.execute(userId, vaccinationId);

        // then
        verify(vaccinationRepository).softDelete(eq(vaccinationId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("読み取り後に削除済みとなり更新0件の場合、VACCINATION-001例外が発生する")
    void delete_vaccination_fail_when_deleted_concurrently() {
        // given
        givenOwnedVaccination(VaccinationStatus.SCHEDULED);
        when(vaccinationRepository.softDelete(eq(vaccinationId), any())).thenReturn(false);

        // when & then
        assertErrorCode(() -> deleteVaccinationService.execute(userId, vaccinationId),
                VaccinationErrorCode.VACCINATION_NOT_FOUND);
    }

    @Test
    @DisplayName("他ユーザーのペットの接種予定削除時、VACCINATION-002例外が発生する")
    void delete_vaccination_fail_when_not_pet_owner() {
        // given
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.of(vaccination(VaccinationStatus.SCHEDULED)));
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        // when & then
        assertErrorCode(() -> deleteVaccinationService.execute(userId, vaccinationId),
                VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).softDelete(anyLong(), any());
    }

    // -------------------------------------------------------------- reminders

    @Test
    @DisplayName("SCHD-T002: 当日〜7日後の対象ごとにVACCINATION通知を作成する")
    void send_reminders_creates_vaccination_notifications() {
        // given
        LocalDate today = LocalDate.of(2026, 10, 5);
        when(vaccinationRepository.findReminderTargets(today, LocalDate.of(2026, 10, 12))).thenReturn(List.of(
                new VaccinationReminderTarget(vaccinationId, userId, "Momo", "混合ワクチン", LocalDate.of(2026, 10, 10))
        ));
        when(vaccinationRepository.markReminded(vaccinationId, LocalDate.of(2026, 10, 10))).thenReturn(true);

        // when
        int created = sendVaccinationRemindersService.execute(today);

        // then
        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getVaccinationId()).isEqualTo(vaccinationId);
        assertThat(notification.getReservationId()).isNull();
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.VACCINATION);
        assertThat(notification.getMessage()).isEqualTo("Momoの混合ワクチン接種予定日（2026-10-10）が近づいています。");
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("抽出後に中止・再登録などが確定し送信済みにできない対象は通知しない（REVIEW-001 R-03）")
    void send_reminders_skips_target_changed_after_extraction() {
        // given
        LocalDate today = LocalDate.of(2026, 10, 5);
        LocalDate changedDate = LocalDate.of(2026, 10, 10);
        when(vaccinationRepository.findReminderTargets(any(), any())).thenReturn(List.of(
                new VaccinationReminderTarget(vaccinationId, userId, "Momo", "混合ワクチン", changedDate),
                new VaccinationReminderTarget(101L, userId, "Momo", "狂犬病", LocalDate.of(2026, 10, 11))
        ));
        when(vaccinationRepository.markReminded(vaccinationId, changedDate)).thenReturn(false);
        when(vaccinationRepository.markReminded(101L, LocalDate.of(2026, 10, 11))).thenReturn(true);

        // when
        int created = sendVaccinationRemindersService.execute(today);

        // then
        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getVaccinationId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("対象が無い場合、通知を作成しない")
    void send_reminders_creates_nothing_without_targets() {
        // given
        when(vaccinationRepository.findReminderTargets(any(), any())).thenReturn(List.of());

        // when & then
        assertThat(sendVaccinationRemindersService.execute(LocalDate.of(2026, 10, 5))).isZero();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // ------------------------------------- 追加: 遷移競合・ペット消失（REVIEW-001 G-04 / G-10）

    @Test
    @DisplayName("読み取り後に別の遷移が確定し更新0件の場合、再登録はVACCINATION-003例外が発生する")
    void reschedule_vaccination_fail_when_transition_lost() {
        // given
        givenOwnedVaccination(VaccinationStatus.CANCELLED);
        when(vaccinationRepository.reschedule(eq(vaccinationId), any(), any())).thenReturn(false);

        // when & then
        assertErrorCode(() -> rescheduleVaccinationService.execute(userId, vaccinationId, LocalDate.of(2026, 11, 1)),
                VaccinationErrorCode.INVALID_STATE_TRANSITION);

        verify(vaccinationRepository).reschedule(eq(vaccinationId), eq(LocalDate.of(2026, 11, 1)), any(LocalDateTime.class));
        verify(vaccinationRepository, never()).save(any(Vaccination.class));
    }

    private void givenVaccinationWithMissingPet(VaccinationStatus status) {
        when(vaccinationRepository.findById(vaccinationId)).thenReturn(Optional.of(vaccination(status)));
        when(petRepository.findById(petId)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("接種予定は存在するがペットが取得できない場合、完了はVACCINATION-002例外が発生する")
    void complete_vaccination_fail_when_pet_not_found() {
        // given
        givenVaccinationWithMissingPet(VaccinationStatus.SCHEDULED);

        // when & then
        assertErrorCode(() -> completeVaccinationService.execute(userId, vaccinationId, null),
                VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).markCompleted(anyLong(), any(), any());
    }

    @Test
    @DisplayName("接種予定は存在するがペットが取得できない場合、中止はVACCINATION-002例外が発生する")
    void cancel_vaccination_fail_when_pet_not_found() {
        // given
        givenVaccinationWithMissingPet(VaccinationStatus.SCHEDULED);

        // when & then
        assertErrorCode(() -> cancelVaccinationService.execute(userId, vaccinationId),
                VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).cancel(anyLong(), any());
    }

    @Test
    @DisplayName("接種予定は存在するがペットが取得できない場合、再登録はVACCINATION-002例外が発生する")
    void reschedule_vaccination_fail_when_pet_not_found() {
        // given
        givenVaccinationWithMissingPet(VaccinationStatus.CANCELLED);

        // when & then
        assertErrorCode(() -> rescheduleVaccinationService.execute(userId, vaccinationId, LocalDate.of(2026, 11, 1)),
                VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).reschedule(anyLong(), any(), any());
    }

    @Test
    @DisplayName("接種予定は存在するがペットが取得できない場合、削除はVACCINATION-002例外が発生する")
    void delete_vaccination_fail_when_pet_not_found() {
        // given
        givenVaccinationWithMissingPet(VaccinationStatus.SCHEDULED);

        // when & then
        assertErrorCode(() -> deleteVaccinationService.execute(userId, vaccinationId),
                VaccinationErrorCode.NOT_PET_OWNER);
        verify(vaccinationRepository, never()).softDelete(anyLong(), any());
    }
}
