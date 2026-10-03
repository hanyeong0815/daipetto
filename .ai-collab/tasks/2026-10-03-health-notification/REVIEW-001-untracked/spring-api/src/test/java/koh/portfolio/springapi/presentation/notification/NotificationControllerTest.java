package koh.portfolio.springapi.presentation.notification;

import koh.portfolio.springapi.application.notification.dto.NotificationDto.NotificationResponse;
import koh.portfolio.springapi.application.notification.usecase.GetNotificationListUseCase;
import koh.portfolio.springapi.application.notification.usecase.MarkNotificationAsReadUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.notification.model.NotificationType;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetNotificationListUseCase getNotificationListUseCase;

    @MockBean
    private MarkNotificationAsReadUseCase markNotificationAsReadUseCase;

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    @DisplayName("GET /api/v1/notifications - 通知一覧取得成功")
    void get_notification_list_success() throws Exception {
        // given
        when(getNotificationListUseCase.execute(1L)).thenReturn(List.of(
                NotificationResponse.builder()
                        .notificationId(100L)
                        .type(NotificationType.RESERVATION_APPROVED)
                        .title("予約が承認されました")
                        .message("ご予約が承認されました。予約内容をご確認ください。")
                        .isRead(false)
                        .createdAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                        .build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/notifications").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].notificationId").value(100))
                .andExpect(jsonPath("$.data[0].type").value("RESERVATION_APPROVED"))
                .andExpect(jsonPath("$.data[0].title").value("予約が承認されました"))
                .andExpect(jsonPath("$.data[0].isRead").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{notificationId}/read - 通知既読処理成功")
    void mark_notification_as_read_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/notifications/100/read").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(markNotificationAsReadUseCase).execute(1L, 100L);
    }
}
