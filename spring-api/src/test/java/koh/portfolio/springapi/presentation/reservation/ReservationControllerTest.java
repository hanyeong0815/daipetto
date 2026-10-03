package koh.portfolio.springapi.presentation.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationRequest;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationResponse;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationDetailResponse;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationSummary;
import koh.portfolio.springapi.application.reservation.usecase.CancelReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.CreateReservationUseCase;
import koh.portfolio.springapi.application.reservation.usecase.GetReservationDetailUseCase;
import koh.portfolio.springapi.application.reservation.usecase.GetReservationListUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReservationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateReservationUseCase createReservationUseCase;

    @MockBean
    private GetReservationListUseCase getReservationListUseCase;

    @MockBean
    private GetReservationDetailUseCase getReservationDetailUseCase;

    @MockBean
    private CancelReservationUseCase cancelReservationUseCase;

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("POST /api/v1/reservations - 予約申請成功")
    void create_reservation_success() throws Exception {
        // given
        CreateReservationRequest request = new CreateReservationRequest(1L, 10L, 100L, "咳があります");

        when(createReservationUseCase.execute(eq(1L), any(CreateReservationRequest.class)))
                .thenReturn(CreateReservationResponse.builder().reservationId(1L).status(ReservationStatus.REQUESTED).build());

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationId").value(1))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations - petIdがnullの場合Validation失敗")
    void create_reservation_validation_fail_when_pet_id_null() throws Exception {
        // given
        CreateReservationRequest request = new CreateReservationRequest(null, 10L, 100L, null);

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    // ---------------------------------------------------------------- getList

    @Test
    @DisplayName("GET /api/v1/reservations - 予約一覧取得成功")
    void get_reservation_list_success() throws Exception {
        // given
        List<ReservationSummary> summaries = List.of(
                ReservationSummary.builder()
                        .reservationId(1L).hospitalName("Tokyo Animal Hospital").petName("Momo")
                        .availableDate(LocalDate.of(2026, 5, 20)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                        .status(ReservationStatus.REQUESTED)
                        .build()
        );

        when(getReservationListUseCase.execute(1L)).thenReturn(summaries);

        // when & then
        mockMvc.perform(get("/api/v1/reservations").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].reservationId").value(1))
                .andExpect(jsonPath("$.data[0].hospitalName").value("Tokyo Animal Hospital"));
    }

    // --------------------------------------------------------------- getDetail

    @Test
    @DisplayName("GET /api/v1/reservations/{reservationId} - 予約詳細取得成功")
    void get_reservation_detail_success() throws Exception {
        // given
        ReservationDetailResponse response = ReservationDetailResponse.builder()
                .reservationId(1L).hospitalName("Tokyo Animal Hospital").petName("Momo")
                .availableDate(LocalDate.of(2026, 5, 20)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                .status(ReservationStatus.REQUESTED).memo("咳があります")
                .build();

        when(getReservationDetailUseCase.execute(1L, 1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/reservations/1").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationId").value(1))
                .andExpect(jsonPath("$.data.memo").value("咳があります"));
    }

    // ----------------------------------------------------------------- cancel

    @Test
    @DisplayName("PATCH /api/v1/reservations/{reservationId}/cancel - 予約キャンセル成功")
    void cancel_reservation_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/reservations/1/cancel").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cancelReservationUseCase).execute(1L, 1L);
    }
}
