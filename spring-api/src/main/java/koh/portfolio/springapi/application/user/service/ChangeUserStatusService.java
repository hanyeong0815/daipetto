package koh.portfolio.springapi.application.user.service;

import koh.portfolio.springapi.application.user.usecase.ChangeUserStatusUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChangeUserStatusService implements ChangeUserStatusUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void execute(Long adminId, Long userId, UserStatus status) {
        // 自分自身を停止すると管理者が操作できなくなるため禁止する
        Preconditions.validate(!adminId.equals(userId), UserErrorCode.CANNOT_CHANGE_SELF);

        // ログイン・トークン再発行と同じユーザー行の排他を取り、停止の確定後に新しいトークンが残らないようにする
        userRepository.lockForSessionUpdate(userId);

        // 同じ状態への変更は成功として扱う（停止・復旧の二重実行で失敗させない）
        Preconditions.validate(
                userRepository.updateStatus(userId, status, LocalDateTime.now()),
                UserErrorCode.NO_SUCH_USER
        );

        // 停止したユーザーの Refresh Token をすべて失効させる。発行済みの Access Token も
        // JwtAuthenticationFilter が現在の状態を確認するため、次のリクエストから使えなくなる
        if (status == UserStatus.SUSPENDED) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }
    }
}
