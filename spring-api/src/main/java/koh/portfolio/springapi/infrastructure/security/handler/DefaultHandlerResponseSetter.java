package koh.portfolio.springapi.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class DefaultHandlerResponseSetter {
    public void defaultResponseSetter(HttpServletResponse response, ObjectMapper objectMapper, AuthErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.defaultHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> body = ApiResponse.error(
                errorCode.code(),
                errorCode.defaultMessage()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}
