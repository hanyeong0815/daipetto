package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.reservation.model.AdminReservationView;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.infrastructure.persistence.hospital.HospitalScheduleEntity;
import koh.portfolio.springapi.infrastructure.persistence.pet.PetEntity;
import koh.portfolio.springapi.infrastructure.persistence.user.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 病院管理者向け予約一覧（SC-013）のJPQLを実DB（H2）で実行する
@DataJpaTest
@ActiveProfiles("test")
@Import({
        ReservationPersistenceAdapter.class,
        ReservationMapperImpl.class
})
class ReservationAdminQueryTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 20, 9, 0);
    private static final LocalDate DAY1 = LocalDate.of(2026, 10, 25);
    private static final LocalDate DAY2 = LocalDate.of(2026, 10, 26);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TestEntityManager em;

    private Long user(String nickname) {
        return em.persist(UserEntity.builder().email(nickname + "@example.com").password("pw").nickname(nickname)
                .role(Role.ROLE_USER).status(UserStatus.ACTIVE).createdAt(AT).updatedAt(AT).build()).getId();
    }

    private Long pet(Long userId, String name, LocalDateTime deletedAt) {
        return em.persist(PetEntity.builder().userId(userId).name(name).petType(PetType.CAT).gender(PetGender.FEMALE)
                .createdAt(AT).updatedAt(AT).deletedAt(deletedAt).build()).getId();
    }

    private Long schedule(Long hospitalId, LocalDate date, LocalTime start) {
        return em.persist(HospitalScheduleEntity.builder().hospitalId(hospitalId).availableDate(date).startTime(start)
                .endTime(start.plusMinutes(30)).status(HospitalScheduleStatus.AVAILABLE).createdAt(AT).updatedAt(AT).build()).getId();
    }

    private Long reservation(Long userId, Long petId, Long hospitalId, Long scheduleId, ReservationStatus status, LocalDateTime deletedAt) {
        return em.persist(ReservationEntity.builder().userId(userId).petId(petId).hospitalId(hospitalId).scheduleId(scheduleId)
                .reservationDatetime(AT).status(status).memo("memo").createdAt(AT).updatedAt(AT).deletedAt(deletedAt).build()).getId();
    }

    @Test
    @DisplayName("病院の予約を予約日・開始時刻順に、予約者・ペット付きで返す。状態・日付の絞り込み、null条件、論理削除の除外")
    void find_all_for_admin_filters_and_orders() {
        // given
        Long tanaka = user("Tanaka");
        Long sato = user("Sato");
        Long momo = pet(tanaka, "Momo", null);
        Long kuro = pet(sato, "Kuro", AT); // 予約後に論理削除されたペットも履歴には名前を出す
        Long late = reservation(tanaka, momo, 1L, schedule(1L, DAY2, LocalTime.of(9, 0)), ReservationStatus.REQUESTED, null);
        Long early = reservation(sato, kuro, 1L, schedule(1L, DAY1, LocalTime.of(14, 0)), ReservationStatus.APPROVED, null);
        Long earliest = reservation(tanaka, momo, 1L, schedule(1L, DAY1, LocalTime.of(10, 0)), ReservationStatus.REQUESTED, null);
        reservation(tanaka, momo, 1L, schedule(1L, DAY1, LocalTime.of(11, 0)), ReservationStatus.REQUESTED, AT); // 論理削除済み
        reservation(tanaka, momo, 2L, schedule(2L, DAY1, LocalTime.of(10, 0)), ReservationStatus.REQUESTED, null); // 別の病院
        em.flush();

        // when & then: 条件なし（null）
        List<AdminReservationView> all = reservationRepository.findAllForAdmin(1L, null, null);
        assertThat(all).extracting(AdminReservationView::reservationId).containsExactly(earliest, early, late);
        assertThat(all.get(1).userNickname()).isEqualTo("Sato");
        assertThat(all.get(1).petName()).isEqualTo("Kuro");
        assertThat(all.get(1).startTime()).isEqualTo(LocalTime.of(14, 0));

        // 状態・日付での絞り込み
        assertThat(reservationRepository.findAllForAdmin(1L, ReservationStatus.REQUESTED, null))
                .extracting(AdminReservationView::reservationId).containsExactly(earliest, late);
        assertThat(reservationRepository.findAllForAdmin(1L, null, DAY1))
                .extracting(AdminReservationView::reservationId).containsExactly(earliest, early);
        assertThat(reservationRepository.findAllForAdmin(1L, ReservationStatus.APPROVED, DAY2)).isEmpty();
    }
}
