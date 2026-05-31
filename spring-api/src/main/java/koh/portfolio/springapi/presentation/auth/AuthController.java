package koh.portfolio.springapi.presentation.auth;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.auth.dto.LoginDto;
import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginResponse;
import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenRequest;
import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenResponse;
import koh.portfolio.springapi.application.auth.usecase.LoginUseCase;
import koh.portfolio.springapi.application.auth.usecase.RefreshTokenUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody final LoginDto.LoginRequest request) {
        LoginResponse response = loginUseCase.execute(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
            ) {
        RefreshTokenResponse response = refreshTokenUseCase.execute(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
