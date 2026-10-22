package koh.portfolio.springapi.application.user.service;

import koh.portfolio.springapi.application.user.dto.AdminUserDto.AdminUserResponse;
import koh.portfolio.springapi.application.user.usecase.SearchUsersUseCase;
import koh.portfolio.springapi.common.response.PageResponse;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchUsersService implements SearchUsersUseCase {
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> execute(String keyword, Role role, UserStatus status, int page, int size) {
        // 範囲外のページ指定はエラーにせず丸める（page<0 → 0、size は 1〜100、0以下は既定の20）
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

        List<AdminUserResponse> content = userRepository.search(safeKeyword, role, status, safePage, safeSize).stream()
                .map(user -> AdminUserResponse.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.of(content, userRepository.count(safeKeyword, role, status), safePage, safeSize);
    }
}
