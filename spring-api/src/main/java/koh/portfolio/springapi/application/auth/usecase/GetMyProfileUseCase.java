package koh.portfolio.springapi.application.auth.usecase;

import koh.portfolio.springapi.application.user.dto.GetMyProfileDto.MyProfileResponse;

public interface GetMyProfileUseCase {
    MyProfileResponse execute(Long userId);
}
