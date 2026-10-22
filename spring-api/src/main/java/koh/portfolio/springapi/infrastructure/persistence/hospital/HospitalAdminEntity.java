package koh.portfolio.springapi.infrastructure.persistence.hospital;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
// 一意制約は V15 と同じ名前で宣言する（テストのH2スキーマにも作られ、重複の変換をテストできる）
@Table(name = "hospital_admins", uniqueConstraints = @UniqueConstraint(
        name = "uq_hospital_admins_user_hospital", columnNames = {"user_id", "hospital_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HospitalAdminEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long hospitalId;
    private LocalDateTime createdAt;
}
