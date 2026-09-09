package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.usecase.GetMyProfileUseCase;
import koh.portfolio.springapi.application.user.dto.GetMyProfileDto.MyProfileResponse;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMyProfileService implements GetMyProfileUseCase {
    private final UserRepository userRepository;

    @Override
    public MyProfileResponse execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserErrorCode.NO_SUCH_USER::defaultException);

        return MyProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
