package koh.portfolio.springapi.domain.hospital.port;

import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HospitalRepository {
    Hospital save(Hospital hospital);
    Optional<Hospital> findById(Long id);
    List<Hospital> search(String keyword, String area);
    boolean existsById(Long id);

    // 情報更新と停止は別々の列だけを書く。エンティティ全体を保存すると、
    // 同時に行われた停止を情報更新が ACTIVE で上書きする（逆も同様）
    boolean updateInfo(Hospital hospital);
    boolean updateStatus(Long hospitalId, HospitalStatus status, LocalDateTime updatedAt);
}
