package koh.portfolio.springapi.presentation.reservation;

import koh.portfolio.springapi.application.reservation.usecase.ApproveReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.CompleteReservationUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReservationAdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReservationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApproveReservationUseCase approveReservationUseCase;

    @MockBean
    private CompleteReservationUseCase completeReservationUseCase;

    @Test
    @DisplayName("PATCH /api/v1/admin/reservations/{reservationId}/approve - 予約承認成功")
    void approve_reservation_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/reservations/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approveReservationUseCase).execute(1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/reservations/{reservationId}/complete - 診療完了成功")
    void complete_reservation_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/reservations/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(completeReservationUseCase).execute(1L);
    }
}
