package koh.portfolio.springapi.presentation.user;

import jakarta.validation.Valid;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserRequest;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserResponse;
import koh.portfolio.springapi.application.user.usecase.RegisterUserUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final RegisterUserUseCase registerUserUseCase;

    @PostMapping()
    public ResponseEntity<ApiResponse<RegisterUserResponse>> register(
            @Valid @RequestBody RegisterUserRequest request
            ) {
        RegisterUserResponse response = registerUserUseCase.execute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
