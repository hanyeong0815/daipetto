package koh.portfolio.springapi.application.auth.usecase;

import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginRequest;
import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginResponse;

public interface LoginUseCase {
    LoginResponse execute(LoginRequest request);
}
