-- リマインダー送信済みの接種予定日。送信時に条件付きUPDATEで書き込み、再登録でNULLに戻す（08_State_Design §11-1）
ALTER TABLE vaccinations
    ADD COLUMN reminded_date DATE NULL;

-- 旧方式（最後の変更以降に通知があれば送信済み）で送信済みの予定を引き継ぐ
UPDATE vaccinations v
SET reminded_date = v.scheduled_date
WHERE EXISTS (SELECT 1
              FROM notifications n
              WHERE n.vaccination_id = v.id
                AND n.created_at >= v.updated_at);
