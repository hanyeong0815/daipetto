package koh.portfolio.springapi.application.user.usecase;

import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserRequest;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserResponse;

public interface RegisterUserUseCase {
    RegisterUserResponse execute(RegisterUserRequest request);
}
