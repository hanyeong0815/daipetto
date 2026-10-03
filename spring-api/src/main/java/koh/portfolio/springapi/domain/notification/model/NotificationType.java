package koh.portfolio.springapi.domain.notification.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    RESERVATION_APPROVED("予約が承認されました", "ご予約が承認されました。予約内容をご確認ください。"),
    RESERVATION_REJECTED("予約が却下されました", "ご予約が却下されました。別の予約枠をご確認ください。"),
    TREATMENT_COMPLETED("診療が完了しました", "診療が完了しました。健康記録の登録をおすすめします。"),
    // Vaccination ドメイン未実装のため現在は生成されない（08_State_Design §7、文面は07_API_Design §10-1の例に合わせる）
    VACCINATION("ワクチン接種予定", "ワクチン接種予定日が近づいています。");

    private final String title;
    private final String message;
}
