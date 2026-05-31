package koh.portfolio.springapi.presentation.user;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.auth.usecase.GetMyProfileUseCase;
import koh.portfolio.springapi.application.user.dto.GetMyProfileDto.GetMyProfileResponse;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserRequest;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserResponse;
import koh.portfolio.springapi.application.user.usecase.RegisterUserUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final RegisterUserUseCase registerUserUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;

    @PostMapping()
    public ResponseEntity<ApiResponse<RegisterUserResponse>> register(
            @Valid @RequestBody RegisterUserRequest request
            ) {
        RegisterUserResponse response = registerUserUseCase.execute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetMyProfileResponse>> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        GetMyProfileResponse response = getMyProfileUseCase.execute(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
