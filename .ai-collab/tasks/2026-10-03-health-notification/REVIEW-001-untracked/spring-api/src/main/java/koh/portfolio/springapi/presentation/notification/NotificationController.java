package koh.portfolio.springapi.presentation.notification;

import koh.portfolio.springapi.application.notification.dto.NotificationDto.NotificationResponse;
import koh.portfolio.springapi.application.notification.usecase.GetNotificationListUseCase;
import koh.portfolio.springapi.application.notification.usecase.MarkNotificationAsReadUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final GetNotificationListUseCase getNotificationListUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationList(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getNotificationListUseCase.execute(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        Long userId = extractUserId(authentication);
        markNotificationAsReadUseCase.execute(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
