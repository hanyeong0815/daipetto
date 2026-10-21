package koh.portfolio.springapi.domain.pet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Pet {
    private Long id;
    private Long userId;
    private String name;
    private PetType petType;
    private LocalDate birthDate;
    private PetGender gender;
    private BigDecimal weight;
    private String breed;
    private boolean neutered;
    private String microchipNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Pet create(Long userId, String name, PetType petType, LocalDate birthDate, PetGender gender, BigDecimal weight,
                              String breed, boolean neutered, String microchipNumber) {
        LocalDateTime now = LocalDateTime.now();

        return new Pet(null, userId, name, petType, birthDate, gender, weight, breed, neutered, microchipNumber, now, now, null);
    }

    public Pet update(String name, PetType petType, LocalDate birthDate, PetGender gender, BigDecimal weight,
                       String breed, boolean neutered, String microchipNumber) {
        return new Pet(this.id, this.userId, name, petType, birthDate, gender, weight, breed, neutered, microchipNumber,
                this.createdAt, LocalDateTime.now(), null);
    }
}
