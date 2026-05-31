package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginRequest;
import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginResponse;
import koh.portfolio.springapi.application.auth.usecase.LoginUseCase;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import koh.portfolio.springapi.domain.auth.model.RefreshToken;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static koh.portfolio.springapi.common.exception.Preconditions.validate;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public LoginResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(AuthErrorCode.AUTH_FAILED::defaultException);

        validate(
                !user.isSuspended(),
                AuthErrorCode.SUSPENDED_ACCOUNT
        );

        validate(
                passwordEncoder.matches(request.password(), user.getPassword()),
                AuthErrorCode.AUTH_FAILED
        );

        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        String refreshTokenValue = jwtProvider.createRefreshToken(user.getId());

        refreshTokenRepository.revokeAllByUserId(user.getId());

        RefreshToken refreshToken = RefreshToken.issue(
                user.getId(),
                refreshTokenValue,
                jwtProvider.getRefreshTokenExpiresAt()
        );

        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .role(user.getRole())
                .build();
    }
}
