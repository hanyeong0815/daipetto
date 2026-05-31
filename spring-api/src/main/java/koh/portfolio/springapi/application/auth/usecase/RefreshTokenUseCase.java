package koh.portfolio.springapi.application.auth.usecase;

import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenRequest;
import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenResponse;

public interface RefreshTokenUseCase {
    RefreshTokenResponse execute(RefreshTokenRequest request);
}
