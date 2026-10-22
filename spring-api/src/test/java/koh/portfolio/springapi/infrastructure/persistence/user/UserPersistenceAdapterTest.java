package koh.portfolio.springapi.infrastructure.persistence.user;

import jakarta.persistence.EntityManager;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        UserPersistenceAdapter.class,
        UserMapperImpl.class
})
class UserPersistenceAdapterTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("セッション更新用のユーザー行排他取得が実DBで実行できる")
    void lock_for_session_update_runs_against_database() {
        // given
        LocalDateTime now = LocalDateTime.now();
        UserEntity saved = userJpaRepository.save(UserEntity.builder()
                .email("lock@example.com")
                .password("$2a$encoded-password")
                .nickname("locker")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        // when & then
        assertThatCode(() -> userRepository.lockForSessionUpdate(saved.getId())).doesNotThrowAnyException();
        assertThatCode(() -> userRepository.lockForSessionUpdate(999_999L)).doesNotThrowAnyException();
    }

    private UserEntity persist(String email, String nickname, Role role, UserStatus status, LocalDateTime deletedAt) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        return userJpaRepository.save(UserEntity.builder()
                .email(email)
                .password("$2a$encoded-password")
                .nickname(nickname)
                .role(role)
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .deletedAt(deletedAt)
                .build());
    }

    @Test
    @DisplayName("管理者向け検索: メール・ニックネームの部分一致（大文字小文字無視）、権限・状態の絞り込み、論理削除の除外、ページング")
    void search_filters_and_pages() {
        // given
        UserEntity tanaka = persist("tanaka@example.com", "Tanaka", Role.ROLE_USER, UserStatus.ACTIVE, null);
        UserEntity hospital = persist("admin@happy-vet.jp", "HappyVet", Role.ROLE_HOSPITAL_ADMIN, UserStatus.ACTIVE, null);
        UserEntity suspended = persist("sato@example.com", "Sato", Role.ROLE_USER, UserStatus.SUSPENDED, null);
        persist("gone@example.com", "Gone", Role.ROLE_USER, UserStatus.ACTIVE, LocalDateTime.of(2026, 10, 2, 0, 0));

        // when & then: 条件なしは論理削除済み以外をID昇順
        assertThat(userRepository.search(null, null, null, 0, 10)).extracting(User::getId)
                .containsExactly(tanaka.getId(), hospital.getId(), suspended.getId());
        assertThat(userRepository.count(null, null, null)).isEqualTo(3);

        // キーワードはメール・ニックネームのどちらにも、大文字小文字を区別せず一致する
        assertThat(userRepository.search("EXAMPLE", null, null, 0, 10)).extracting(User::getId)
                .containsExactly(tanaka.getId(), suspended.getId());
        assertThat(userRepository.search("happyvet", null, null, 0, 10)).extracting(User::getId)
                .containsExactly(hospital.getId());

        // 権限・状態の絞り込み
        assertThat(userRepository.search(null, Role.ROLE_HOSPITAL_ADMIN, null, 0, 10)).extracting(User::getId)
                .containsExactly(hospital.getId());
        assertThat(userRepository.search(null, null, UserStatus.SUSPENDED, 0, 10)).extracting(User::getId)
                .containsExactly(suspended.getId());
        assertThat(userRepository.count("example", Role.ROLE_USER, UserStatus.ACTIVE)).isEqualTo(1);

        // ページング（page は0始まり）
        assertThat(userRepository.search(null, null, null, 1, 2)).extracting(User::getId)
                .containsExactly(suspended.getId());
    }

    @Test
    @DisplayName("状態の読み直しは、同じ永続化コンテキストに古いUserが残っていてもDBの現在値を返す（REVIEW-001 R-01）")
    void find_status_by_id_bypasses_cached_entity() {
        // given: エンティティを読み込んで永続化コンテキストに載せた後、別経路で状態を変更する
        UserEntity user = persist("cache@example.com", "Cache", Role.ROLE_USER, UserStatus.ACTIVE, null);
        UserEntity cached = userJpaRepository.findById(user.getId()).orElseThrow();
        entityManager.createNativeQuery("update users set status = 'SUSPENDED' where id = :id")
                .setParameter("id", user.getId())
                .executeUpdate();

        // when & then: キャッシュ上のエンティティは古いままだが、列の読み直しは最新
        assertThat(cached.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userRepository.findStatusById(user.getId())).isEqualTo(UserStatus.SUSPENDED);
        assertThat(userRepository.findStatusById(999_999L)).isNull();
    }

    @Test
    @DisplayName("状態・権限の更新は対象の列だけを書き、論理削除済み・存在しないユーザーは0件（false）")
    void update_status_and_role_only_for_active_rows() {
        // given
        UserEntity user = persist("u@example.com", "U", Role.ROLE_USER, UserStatus.ACTIVE, null);
        UserEntity deleted = persist("d@example.com", "D", Role.ROLE_USER, UserStatus.ACTIVE, LocalDateTime.of(2026, 10, 2, 0, 0));
        LocalDateTime changedAt = LocalDateTime.of(2026, 10, 21, 9, 0);

        // when & then
        assertThat(userRepository.updateStatus(user.getId(), UserStatus.SUSPENDED, changedAt)).isTrue();
        assertThat(userRepository.updateRole(user.getId(), Role.ROLE_HOSPITAL_ADMIN, changedAt)).isTrue();
        assertThat(userRepository.updateStatus(deleted.getId(), UserStatus.SUSPENDED, changedAt)).isFalse();
        assertThat(userRepository.updateRole(deleted.getId(), Role.ROLE_SYSTEM_ADMIN, changedAt)).isFalse();
        assertThat(userRepository.updateStatus(999_999L, UserStatus.SUSPENDED, changedAt)).isFalse();

        UserEntity reloaded = userJpaRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(reloaded.getRole()).isEqualTo(Role.ROLE_HOSPITAL_ADMIN);
        assertThat(reloaded.getEmail()).isEqualTo("u@example.com");
        assertThat(userJpaRepository.findById(deleted.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
