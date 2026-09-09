package koh.portfolio.springapi.application.user.service;

import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserRequest;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserResponse;
import koh.portfolio.springapi.application.user.usecase.RegisterUserUseCase;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static koh.portfolio.springapi.common.exception.Preconditions.validate;

@Service
@RequiredArgsConstructor
public class UserService implements RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RegisterUserResponse execute(RegisterUserRequest request) {
        validate(
                !userRepository.existsByEmail(request.email()),
                UserErrorCode.EMAIL_ALREADY_USED
        );

        String hashedPassword = passwordEncoder.encode(request.password());

        User user = User.register(
                request.email(),
                hashedPassword,
                request.nickname()
        );

        User savedUser = userRepository.save(user);

        return RegisterUserResponse.builder()
                .userId(savedUser.getId())
                .build();
    }
}
