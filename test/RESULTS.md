# 単体テスト結果サマリ

- 実施日: 2026-10-23（API単体テストの実行: 2026-10-23T05:26:27+09:00）
- API単体テスト: **134/134 OK**
- 自動テスト（JUnit）: **342/342 合格**（スキップ 0）
- カバレッジ（JaCoCo）: 命令 88.7%（7822/8821）、行（C0相当）86.9%（1659/1910）、分岐（C1相当）76.7%（287/374）

## 機能別

| 機能 | 仕様書・結果 | API単体テスト | 自動テスト（JUnit） |
| --- | --- | --- | --- |
| UT-01 会員・認証 | [UT-01_auth.md](spec/UT-01_auth.md) | 16/16 | 41/41 |
| UT-02 ペット | [UT-02_pet.md](spec/UT-02_pet.md) | 13/13 | 28/28 |
| UT-03 健康記録 | [UT-03_health-record.md](spec/UT-03_health-record.md) | 12/12 | 27/27 |
| UT-04 ワクチン接種予定 | [UT-04_vaccination.md](spec/UT-04_vaccination.md) | 14/14 | 54/54 |
| UT-05 病院 | [UT-05_hospital.md](spec/UT-05_hospital.md) | 11/11 | 27/27 |
| UT-06 病院管理者の紐付け | [UT-06_hospital-admin.md](spec/UT-06_hospital-admin.md) | 8/8 | 8/8 |
| UT-07 診療時間（曜日別） | [UT-07_business-hours.md](spec/UT-07_business-hours.md) | 10/10 | 29/29 |
| UT-08 予約枠 | [UT-08_schedule.md](spec/UT-08_schedule.md) | 10/10 | 29/29 |
| UT-09 予約（利用者） | [UT-09_reservation.md](spec/UT-09_reservation.md) | 13/13 | 32/32 |
| UT-10 予約管理（病院管理者） | [UT-10_reservation-admin.md](spec/UT-10_reservation-admin.md) | 9/9 | 30/30 |
| UT-11 通知 | [UT-11_notification.md](spec/UT-11_notification.md) | 3/3 | 12/12 |
| UT-12 ユーザー管理（システム管理者） | [UT-12_user-admin.md](spec/UT-12_user-admin.md) | 7/7 | 20/20 |
| UT-13 共通（認証・入力・応答形式） | [UT-13_common.md](spec/UT-13_common.md) | 8/8 | 5/5 |
| 合計 | | 134/134 | 342/342 |

画面単体テストは [UT-14_screens.md](spec/UT-14_screens.md)、不具合・指摘は [DEFECTS.md](DEFECTS.md)。

## パッケージ別カバレッジ（JaCoCo）

詳細（クラス・メソッド・行単位）は [evidence/coverage/html/index.html](evidence/coverage/html/index.html)。

| パッケージ | 行（C0相当） | 分岐（C1相当） |
| --- | --- | --- |
| presentation.healthrecord | 93.3% | 50.0% |
| infrastructure.security | 100.0% | —（分岐なし） |
| application.healthrecord.service | 100.0% | 100.0% |
| application.auth.dto | 66.7% | —（分岐なし） |
| presentation.hospital | 92.3% | 50.0% |
| infrastructure.persistence.reservation | 95.2% | 50.0% |
| infrastructure.persistence.hospital | 93.9% | 50.0% |
| infrastructure.security.cors | 100.0% | —（分岐なし） |
| presentation.pet | 94.1% | 50.0% |
| application.notification.service | 100.0% | —（分岐なし） |
| infrastructure.persistence.user | 52.9% | 100.0% |
| domain.auth.exception | 84.6% | —（分岐なし） |
| domain.notification.exception | 50.0% | —（分岐なし） |
| domain.vaccination.model | 100.0% | 100.0% |
| common.response | 100.0% | —（分岐なし） |
| infrastructure.scheduler | 100.0% | —（分岐なし） |
| domain.hospital.model | 100.0% | 95.5% |
| infrastructure.persistence.pet | 45.8% | 100.0% |
| domain.reservation.model | 100.0% | 100.0% |
| domain.notification.model | 100.0% | —（分岐なし） |
| application.hospital.dto | 83.3% | —（分岐なし） |
| infrastructure.persistence.notification | 0.0% | 0.0% |
| infrastructure.persistence.auth | 92.6% | 50.0% |
| application.pet.dto | 83.3% | —（分岐なし） |
| common.time | 57.1% | —（分岐なし） |
| application.reservation.service | 100.0% | 100.0% |
| application.user.service | 100.0% | 100.0% |
| infrastructure.security.handler | 11.1% | —（分岐なし） |
| domain.vaccination.exception | 90.0% | —（分岐なし） |
| application.vaccination.service | 100.0% | 100.0% |
| presentation.auth | 100.0% | —（分岐なし） |
| domain.pet.model | 100.0% | —（分岐なし） |
| application.hospital.service | 100.0% | 100.0% |
| domain.healthrecord.model | 100.0% | 100.0% |
| infrastructure.persistence.healthrecord | 94.4% | 50.0% |
| application.healthrecord.dto | 80.0% | —（分岐なし） |
| application.notification.dto | 50.0% | —（分岐なし） |
| infrastructure.persistence.vaccination | 94.9% | 50.0% |
| application.reservation.dto | 83.3% | —（分岐なし） |
| domain.healthrecord.exception | 77.8% | —（分岐なし） |
| domain.hospital.exception | 100.0% | —（分岐なし） |
| domain.user.exception | 91.7% | —（分岐なし） |
| infrastructure.security.jwt | 92.9% | 78.6% |
| domain.user.model | 100.0% | 100.0% |
| application.vaccination.dto | 83.3% | —（分岐なし） |
| presentation.notification | 87.5% | 50.0% |
| application.pet.service | 100.0% | —（分岐なし） |
| application.auth.service | 100.0% | 100.0% |
| presentation.reservation | 92.9% | 50.0% |
| application.user.dto | 50.0% | —（分岐なし） |
| presentation.user | 87.5% | 50.0% |
| domain.pet.exception | 50.0% | —（分岐なし） |
| common.exception | 11.5% | 12.5% |
| domain.auth.model | 50.0% | 0.0% |
| koh.portfolio.springapi | 25.0% | —（分岐なし） |
| presentation.vaccination | 95.2% | 50.0% |
| domain.reservation.exception | 93.8% | —（分岐なし） |
