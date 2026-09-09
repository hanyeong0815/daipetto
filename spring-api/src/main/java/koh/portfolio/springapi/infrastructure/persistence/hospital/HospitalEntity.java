package koh.portfolio.springapi.infrastructure.persistence.hospital;

import jakarta.persistence.*;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "hospitals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HospitalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    private HospitalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
