package koh.portfolio.springapi.application.notification.service;

import koh.portfolio.springapi.application.notification.dto.NotificationDto.NotificationResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.notification.exception.NotificationErrorCode;
import koh.portfolio.springapi.domain.notification.model.Notification;
import koh.portfolio.springapi.domain.notification.model.NotificationType;
import koh.portfolio.springapi.domain.notification.port.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    private final Long userId = 1L;
    private final Long notificationId = 100L;
    private final Long reservationId = 10L;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
    }

    private Notification notification(Long ownerId, boolean read) {
        LocalDateTime now = LocalDateTime.now();
        return new Notification(notificationId, ownerId, reservationId, null, NotificationType.RESERVATION_APPROVED,
                NotificationType.RESERVATION_APPROVED.getTitle(), NotificationType.RESERVATION_APPROVED.getMessage(),
                read, now, read ? now : null);
    }

    // -------------------------------------------------------------- getList

    @Test
    @DisplayName("NOTI-T004: 通知一覧取得時、ユーザー別の通知を返却する")
    void get_notification_list_success() {
        // given
        when(notificationRepository.findAllByUserId(userId)).thenReturn(List.of(notification(userId, false)));

        // when
        List<NotificationResponse> result = notificationService.execute(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).notificationId()).isEqualTo(notificationId);
        assertThat(result.get(0).type()).isEqualTo(NotificationType.RESERVATION_APPROVED);
        assertThat(result.get(0).title()).isEqualTo("予約が承認されました");
        assertThat(result.get(0).isRead()).isFalse();
    }

    // ----------------------------------------------------------- markAsRead

    @Test
    @DisplayName("NOTI-T005: 通知既読処理時、is_readをtrueに更新する")
    void mark_as_read_success() {
        // given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification(userId, false)));
        when(notificationRepository.markAsRead(anyLong(), any(LocalDateTime.class))).thenReturn(true);

        // when
        notificationService.execute(userId, notificationId);

        // then
        verify(notificationRepository).markAsRead(eq(notificationId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("既読済みの通知を再度既読にしても例外にしない（冪等）")
    void mark_as_read_is_idempotent() {
        // given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification(userId, true)));
        when(notificationRepository.markAsRead(anyLong(), any(LocalDateTime.class))).thenReturn(false);

        // when & then
        notificationService.execute(userId, notificationId);

        verify(notificationRepository).markAsRead(eq(notificationId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("存在しない通知の既読処理時、NOTIFICATION-001例外が発生する")
    void mark_as_read_fail_when_not_found() {
        // given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.execute(userId, notificationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND.code()));

        verify(notificationRepository, never()).markAsRead(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("他ユーザーの通知の既読処理時、NOTIFICATION-002例外が発生する")
    void mark_as_read_fail_when_not_owner() {
        // given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification(999L, false)));

        // when & then
        assertThatThrownBy(() -> notificationService.execute(userId, notificationId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code())
                        .isEqualTo(NotificationErrorCode.NOT_NOTIFICATION_OWNER.code()));

        verify(notificationRepository, never()).markAsRead(anyLong(), any(LocalDateTime.class));
    }

    // ------------------------------------------------------ reservation hook

    @ParameterizedTest
    @CsvSource({
            "RESERVATION_APPROVED, 予約が承認されました",
            "RESERVATION_REJECTED, 予約が却下されました",
            "TREATMENT_COMPLETED, 診療が完了しました"
    })
    @DisplayName("NOTI-T001〜T003: 予約イベント通知は種別に応じたタイトル・本文で未読として作成される")
    void notify_reservation_event_creates_unread_notification(NotificationType type, String expectedTitle) {
        // given
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationService.execute(userId, reservationId, type);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getReservationId()).isEqualTo(reservationId);
        assertThat(saved.getVaccinationId()).isNull();
        assertThat(saved.getNotificationType()).isEqualTo(type);
        assertThat(saved.getTitle()).isEqualTo(expectedTitle);
        assertThat(saved.getMessage()).isEqualTo(type.getMessage());
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getReadAt()).isNull();
    }
}
