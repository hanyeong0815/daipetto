-- 同一予約枠に対してREQUESTED / APPROVEDの予約は1件のみ許可する（06_ERD §12 業務制約）。
-- アプリ側のexists確認だけでは同時リクエストで二重予約が成立するため、DB側で保証する。
CREATE UNIQUE INDEX uq_reservations_active_schedule
    ON reservations (schedule_id)
    WHERE status IN ('REQUESTED', 'APPROVED') AND deleted_at IS NULL;
