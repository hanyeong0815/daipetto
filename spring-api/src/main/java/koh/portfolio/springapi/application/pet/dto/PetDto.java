package koh.portfolio.springapi.application.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetDto() {
    public record CreatePetRequest(
            @NotBlank String name,
            @NotNull PetType petType,
            LocalDate birthDate,
            PetGender gender,
            BigDecimal weight,
            String breed,
            boolean neutered,
            @Size(max = 15) String microchipNumber
    ) {}

    @Builder
    public record CreatePetResponse(Long petId) {}

    @Builder
    public record PetSummary(Long id, String name, PetType petType, BigDecimal weight, String breed) {}

    public record PetDetailResponse(
            Long id,
            String name,
            PetType petType,
            LocalDate birthDate,
            PetGender gender,
            BigDecimal weight,
            String breed,
            boolean neutered,
            String microchipNumber,
            LocalDateTime createdAt
    ) {}

    public record UpdatePetRequest(
            @NotBlank String name,
            @NotNull PetType petType,
            LocalDate birthDate,
            @NotNull PetGender gender,
            BigDecimal weight,
            String breed,
            boolean neutered,
            @Size(max = 15) String microchipNumber
    ) {}
}
