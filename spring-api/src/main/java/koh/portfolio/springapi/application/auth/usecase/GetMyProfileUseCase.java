package koh.portfolio.springapi.application.auth.usecase;

import koh.portfolio.springapi.application.user.dto.GetMyProfileDto.GetMyProfileResponse;

public interface GetMyProfileUseCase {
    GetMyProfileResponse execute(Long userId);
}
