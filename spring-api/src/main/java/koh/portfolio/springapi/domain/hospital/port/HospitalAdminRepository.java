package koh.portfolio.springapi.domain.hospital.port;

import koh.portfolio.springapi.domain.hospital.model.HospitalAdmin;
import koh.portfolio.springapi.domain.hospital.model.HospitalAdminSummary;

import java.util.List;

public interface HospitalAdminRepository {
    boolean exists(Long userId, Long hospitalId);

    // 一意制約（user_id, hospital_id）違反は HOSPITAL-007 に変換する（同時登録で負けた側を含む）
    HospitalAdmin save(HospitalAdmin hospitalAdmin);

    // 削除できたかを返す（紐付けが無ければ false）
    boolean delete(Long userId, Long hospitalId);

    // 権限変更で病院管理者でなくなったユーザーの紐付けをすべて外す
    void deleteAllByUserId(Long userId);

    List<Long> findHospitalIdsByUserId(Long userId);

    List<HospitalAdminSummary> findAdminsByHospitalId(Long hospitalId);
}
