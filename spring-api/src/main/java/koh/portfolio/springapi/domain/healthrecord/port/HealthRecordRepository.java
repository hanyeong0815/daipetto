package koh.portfolio.springapi.domain.healthrecord.port;

import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository {
    HealthRecord save(HealthRecord healthRecord);
    Optional<HealthRecord> findById(Long id);

    // 部分更新の読み取り〜書き込みを直列化するための排他取得（論理削除済みは対象外）。
    // ロックせずに読むと、読み取り後にcommitされた他の更新を、リクエストに無い列まで古い値で上書きしてしまう
    Optional<HealthRecord> findByIdForUpdate(Long id);

    List<HealthRecord> findAllByPetId(Long petId);
    void softDelete(Long id, LocalDateTime deletedAt);

    // 内容列だけを、論理削除されていない行に限って更新する。更新できたかを返す。
    // エンティティ全体を保存すると、読み取り後にcommitされた論理削除を deleted_at=NULL で上書きしてしまう
    boolean updateContent(HealthRecord healthRecord);
}
