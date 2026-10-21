package koh.portfolio.springapi.domain.vaccination.port;

import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationReminderTarget;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VaccinationRepository {
    Vaccination save(Vaccination vaccination);
    Optional<Vaccination> findById(Long id);
    List<Vaccination> findAllByPetId(Long petId);

    // 状態遷移は遷移元の状態を WHERE 条件にした列単位のUPDATEで書き込み、更新できたかを返す。
    // 同時に別の遷移が成立していた場合は false となる（エンティティ全体の保存はしない）
    boolean markCompleted(Long id, LocalDate completedDate, LocalDateTime updatedAt);
    boolean reschedule(Long id, LocalDate scheduledDate, LocalDateTime updatedAt);
    boolean cancel(Long id, LocalDateTime updatedAt);

    // 論理削除されていない行だけを削除する。同時に削除済みなら false
    boolean softDelete(Long id, LocalDateTime deletedAt);

    // 接種予定日が from〜to の SCHEDULED のうち、その予定日でまだ通知していないもの
    List<VaccinationReminderTarget> findReminderTargets(LocalDate from, LocalDate to);

    // 抽出した予定日のまま予定中で未送信なら送信済みにし、true を返す。
    // 抽出後に中止・再登録などが確定していれば false（古い予定日の通知を作らない）
    boolean markReminded(Long id, LocalDate scheduledDate);
}
