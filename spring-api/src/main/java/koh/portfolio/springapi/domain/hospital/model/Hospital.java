package koh.portfolio.springapi.domain.hospital.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Hospital {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private HospitalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Hospital create(String name, String address, String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();

        return new Hospital(null, name, address, phoneNumber, HospitalStatus.ACTIVE, now, now, null);
    }
}
