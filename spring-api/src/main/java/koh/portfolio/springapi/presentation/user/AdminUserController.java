package koh.portfolio.springapi.presentation.user;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.user.dto.AdminUserDto.AdminUserResponse;
import koh.portfolio.springapi.application.user.dto.AdminUserDto.ChangeRoleRequest;
import koh.portfolio.springapi.application.user.usecase.ChangeUserRoleUseCase;
import koh.portfolio.springapi.application.user.usecase.ChangeUserStatusUseCase;
import koh.portfolio.springapi.application.user.usecase.SearchUsersUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.common.response.PageResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// SC-015 ユーザー管理（システム管理者のみ）
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminUserController {
    private final SearchUsersUseCase searchUsersUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(searchUsersUseCase.execute(keyword, role, status, page, size)));
    }

    @PatchMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(Authentication authentication, @PathVariable Long userId) {
        changeUserStatusUseCase.execute(extractUserId(authentication), userId, UserStatus.SUSPENDED);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(Authentication authentication, @PathVariable Long userId) {
        changeUserStatusUseCase.execute(extractUserId(authentication), userId, UserStatus.ACTIVE);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        changeUserRoleUseCase.execute(extractUserId(authentication), userId, request.role());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}
