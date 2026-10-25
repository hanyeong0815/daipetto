package koh.portfolio.springapi.presentation.reservation;

import koh.portfolio.springapi.application.reservation.usecase.ApproveReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.CompleteReservationUseCase;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.AdminReservationResponse;
import koh.portfolio.springapi.application.reservation.usecase.GetAdminReservationListUseCase;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.application.reservation.usecase.RejectReservationUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockitoBean
    private ApproveReservationUseCase approveReservationUseCase;

    @MockitoBean
    private CompleteReservationUseCase completeReservationUseCase;

    @MockitoBean
    private RejectReservationUseCase rejectReservationUseCase;

    @MockitoBean
    private GetAdminReservationListUseCase getAdminReservationListUseCase;

    @Test
    @DisplayName("PATCH /api/v1/admin/reservations/{reservationId}/approve - 予約承認成功")
    void approve_reservation_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/reservations/1/approve").principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(approveReservationUseCase).execute(99L, 1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/reservations/{reservationId}/complete - 診療完了成功")
    void complete_reservation_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/reservations/1/complete").principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(completeReservationUseCase).execute(99L, 1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/reservations/{reservationId}/reject - 予約却下成功")
    void reject_reservation_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/reservations/1/reject").principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(rejectReservationUseCase).execute(99L, 1L);
    }


    @Test
    @DisplayName("GET /api/v1/admin/reservations - 病院・状態・日付を渡し、予約者・ペット付きの一覧を返す")
    void get_admin_reservation_list_success() throws Exception {
        // given
        when(getAdminReservationListUseCase.execute(99L, 1L, ReservationStatus.REQUESTED, LocalDate.of(2026, 10, 25)))
                .thenReturn(List.of(AdminReservationResponse.builder()
                        .reservationId(7L).status(ReservationStatus.REQUESTED)
                        .availableDate(LocalDate.of(2026, 10, 25)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                        .userId(3L).userNickname("Tanaka").petName("Momo").petType(PetType.CAT).memo("咳")
                        .createdAt(LocalDateTime.of(2026, 10, 20, 9, 0)).build()));

        // when & then
        mockMvc.perform(get("/api/v1/admin/reservations").principal(admin())
                        .param("hospitalId", "1").param("status", "REQUESTED").param("date", "2026-10-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reservationId").value(7))
                .andExpect(jsonPath("$.data[0].userNickname").value("Tanaka"))
                .andExpect(jsonPath("$.data[0].petName").value("Momo"))
                .andExpect(jsonPath("$.data[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-10-20T09:00:00+09:00"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reservations - hospitalIdなし・不正な日付は400、管理対象外は403（HOSPITAL-005）")
    void get_admin_reservation_list_errors() throws Exception {
        // given
        doThrow(HospitalErrorCode.NOT_HOSPITAL_MANAGER.defaultException())
                .when(getAdminReservationListUseCase).execute(99L, 2L, null, null);

        // when & then
        mockMvc.perform(get("/api/v1/admin/reservations").principal(admin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/reservations").principal(admin()).param("hospitalId", "1").param("date", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        mockMvc.perform(get("/api/v1/admin/reservations").principal(admin()).param("hospitalId", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOSPITAL-005"));
    }
    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(99L, null, List.of());
    }
}
