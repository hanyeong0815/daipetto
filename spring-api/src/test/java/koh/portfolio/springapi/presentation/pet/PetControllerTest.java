package koh.portfolio.springapi.presentation.pet;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetRequest;
import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetResponse;
import koh.portfolio.springapi.application.pet.dto.PetDto.PetDetailResponse;
import koh.portfolio.springapi.application.pet.dto.PetDto.PetSummary;
import koh.portfolio.springapi.application.pet.dto.PetDto.UpdatePetRequest;
import koh.portfolio.springapi.application.pet.usecase.CreatePetUseCase;
import koh.portfolio.springapi.application.pet.usecase.DeletePetUseCase;
import koh.portfolio.springapi.application.pet.usecase.GetPetDetailUseCase;
import koh.portfolio.springapi.application.pet.usecase.GetPetListUseCase;
import koh.portfolio.springapi.application.pet.usecase.UpdatePetUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatePetUseCase createPetUseCase;

    @MockBean
    private GetPetListUseCase getPetListUseCase;

    @MockBean
    private GetPetDetailUseCase getPetDetailUseCase;

    @MockBean
    private UpdatePetUseCase updatePetUseCase;

    @MockBean
    private DeletePetUseCase deletePetUseCase;

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("POST /api/v1/pets - ペット登録成功")
    void create_pet_success() throws Exception {
        // given
        CreatePetRequest request = new CreatePetRequest(
                "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345"
        );

        when(createPetUseCase.execute(eq(1L), any(CreatePetRequest.class)))
                .thenReturn(new CreatePetResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/pets")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.petId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/pets - 性別は任意項目のため省略しても登録できる")
    void create_pet_without_gender_success() throws Exception {
        // given
        when(createPetUseCase.execute(eq(1L), any(CreatePetRequest.class)))
                .thenReturn(new CreatePetResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/pets")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Momo\",\"petType\":\"CAT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.petId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/pets - nameが空白の場合Validation失敗")
    void create_pet_validation_fail_when_name_blank() throws Exception {
        // given
        CreatePetRequest request = new CreatePetRequest(
                "", PetType.CAT, null, PetGender.FEMALE, null, null, false, null
        );

        // when & then
        mockMvc.perform(post("/api/v1/pets")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    @Test
    @DisplayName("POST /api/v1/pets - 名前51文字・品種101文字・負の体重・桁数超過の体重はValidation失敗（列の長さ・DECIMAL(5,2)）")
    void create_pet_validation_fail_when_breed_too_long_or_weight_invalid() throws Exception {
        for (CreatePetRequest request : List.of(
                new CreatePetRequest("a".repeat(51), PetType.CAT, null, PetGender.FEMALE, null, null, false, null),
                new CreatePetRequest("Momo", PetType.CAT, null, PetGender.FEMALE, null, "a".repeat(101), false, null),
                new CreatePetRequest("Momo", PetType.CAT, null, PetGender.FEMALE, new BigDecimal("-0.1"), null, false, null),
                new CreatePetRequest("Momo", PetType.CAT, null, PetGender.FEMALE, new BigDecimal("1000.00"), null, false, null)
        )) {
            mockMvc.perform(post("/api/v1/pets")
                            .principal(auth(1L))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }
        verifyNoInteractions(createPetUseCase);
    }

    @Test
    @DisplayName("POST /api/v1/pets - petTypeがnullの場合Validation失敗")
    void create_pet_validation_fail_when_pettype_null() throws Exception {
        // given
        CreatePetRequest request = new CreatePetRequest(
                "Momo", null, null, PetGender.FEMALE, null, null, false, null
        );

        // when & then
        mockMvc.perform(post("/api/v1/pets")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    // ---------------------------------------------------------------- getList

    @Test
    @DisplayName("GET /api/v1/pets - ペット一覧取得成功")
    void get_pet_list_success() throws Exception {
        // given
        List<PetSummary> summaries = List.of(
                new PetSummary(1L, "Momo", PetType.CAT, new BigDecimal("4.5"), "Scottish Fold"),
                new PetSummary(2L, "Kuma", PetType.DOG, new BigDecimal("8.0"), "Shiba")
        );

        when(getPetListUseCase.execute(1L)).thenReturn(summaries);

        // when & then
        mockMvc.perform(get("/api/v1/pets").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Momo"))
                .andExpect(jsonPath("$.data[0].petType").value("CAT"))
                .andExpect(jsonPath("$.data[0].breed").value("Scottish Fold"))
                .andExpect(jsonPath("$.data[1].name").value("Kuma"))
                .andExpect(jsonPath("$.data[1].petType").value("DOG"))
                .andExpect(jsonPath("$.data[1].breed").value("Shiba"));
    }

    // --------------------------------------------------------------- getDetail

    @Test
    @DisplayName("GET /api/v1/pets/{petId} - ペット詳細取得成功")
    void get_pet_detail_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        PetDetailResponse response = new PetDetailResponse(
                1L, "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("4.5"),
                "Scottish Fold", true, "123456789012345", now
        );

        when(getPetDetailUseCase.execute(1L, 1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/pets/1").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Momo"))
                .andExpect(jsonPath("$.data.petType").value("CAT"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.breed").value("Scottish Fold"))
                .andExpect(jsonPath("$.data.neutered").value(true))
                .andExpect(jsonPath("$.data.microchipNumber").value("123456789012345"));
    }

    // ----------------------------------------------------------------- update

    @Test
    @DisplayName("PATCH /api/v1/pets/{petId} - ペット情報更新成功")
    void update_pet_success() throws Exception {
        // given
        UpdatePetRequest request = new UpdatePetRequest(
                "MomoUpdated", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE, new BigDecimal("5.0"),
                "Scottish Fold", true, "123456789012345"
        );

        // when & then
        mockMvc.perform(patch("/api/v1/pets/1")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(updatePetUseCase).execute(eq(1L), eq(1L), any(UpdatePetRequest.class));
    }

    // ----------------------------------------------------------------- delete

    @Test
    @DisplayName("DELETE /api/v1/pets/{petId} - ペット論理削除成功")
    void delete_pet_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/pets/1").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(deletePetUseCase).execute(1L, 1L);
    }
}
