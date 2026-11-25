package it.somnia.dreamscape.api.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.mission.model.Mission;
import it.somnia.dreamscape.api.mission.model.MissionGoalType;
import it.somnia.dreamscape.api.mission.model.MissionPeriod;
import it.somnia.dreamscape.api.mission.model.UserMission;
import it.somnia.dreamscape.api.mission.repository.MissionRepository;
import it.somnia.dreamscape.api.mission.repository.UserMissionRepository;
import it.somnia.dreamscape.api.user.model.User;
import jakarta.persistence.EntityManager;

// Le due missioni di V9 (con la settimanale solo classificata di V105) lette attraverso il model, e le righe di avanzamento create e cercate dai repository
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MissionRepositoryTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 25);

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final EntityManager entityManager;

    @Autowired
    MissionRepositoryTest(MissionRepository missionRepository, UserMissionRepository userMissionRepository,
            EntityManager entityManager) {
        this.missionRepository = missionRepository;
        this.userMissionRepository = userMissionRepository;
        this.entityManager = entityManager;
    }

    @Test
    void findByActiveTrue_seedMissions_readsDailyAndRankedWeekly() {
        // Given: le due missioni di V9

        // When
        List<Mission> missions = missionRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Mission::getCode))
                .toList();

        // Then
        assertEquals(2, missions.size());
        Mission daily = missions.get(0);
        assertEquals("DAILY_PLAY", daily.getCode());
        assertEquals(MissionPeriod.DAILY, daily.getPeriod());
        assertEquals(MissionGoalType.PLAY_MATCHES, daily.getGoalType());
        assertEquals(1, daily.getGoalCount());
        assertEquals(100, daily.getRewardCredits());
        assertNull(daily.getMatchType());
        Mission weekly = missions.get(1);
        assertEquals("WEEKLY_WIN", weekly.getCode());
        assertEquals(MissionPeriod.WEEKLY, weekly.getPeriod());
        assertEquals(MissionGoalType.WIN_MATCHES, weekly.getGoalType());
        assertEquals(5, weekly.getGoalCount());
        assertEquals(250, weekly.getRewardCredits());
        assertEquals(MatchType.RANKED, weekly.getMatchType());
    }

    @Test
    void insertIfMissing_calledTwice_createsOneRowWithZeroProgress() {
        // Given
        UUID userId = userId("anna_test");
        Mission daily = mission("DAILY_PLAY");

        // When
        userMissionRepository.insertIfMissing(userId, daily.getId(), WEDNESDAY);
        userMissionRepository.insertIfMissing(userId, daily.getId(), WEDNESDAY);

        // Then
        UserMission row = userMissionRepository.findForUpdate(userId, daily.getId(), WEDNESDAY).orElseThrow();
        assertEquals(0, row.getProgress());
        assertFalse(row.isCompleted());
        assertNull(row.getCreditTransaction());
        assertEquals(1, userMissionRepository.findByUserAndPeriodStarts(userId, Set.of(WEDNESDAY)).size());
    }

    @Test
    void findByUserAndPeriodStarts_rowsOfOtherDays_returnsOnlyTheGivenDays() {
        // Given: la settimanale parte lunedì, una giornaliera di lunedì e una di mercoledì
        UUID userId = userId("anna_test");
        Mission daily = mission("DAILY_PLAY");
        Mission weekly = mission("WEEKLY_WIN");
        userMissionRepository.insertIfMissing(userId, weekly.getId(), MONDAY);
        userMissionRepository.insertIfMissing(userId, daily.getId(), MONDAY);
        userMissionRepository.insertIfMissing(userId, daily.getId(), WEDNESDAY);
        userMissionRepository.insertIfMissing(userId("bruno_test"), daily.getId(), WEDNESDAY);

        // When: la Dashboard di mercoledì cerca le righe di oggi e del lunedì della settimana
        List<UserMission> rows = userMissionRepository.findByUserAndPeriodStarts(userId, Set.of(WEDNESDAY, MONDAY));

        // Then: anche la giornaliera di lunedì, che poi il service scarta perché è di un altro periodo
        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.getUser().getId().equals(userId)));
    }

    @Test
    void advance_completedMission_keepsTheProgress() {
        // Given
        UUID userId = userId("anna_test");
        Mission daily = mission("DAILY_PLAY");
        userMissionRepository.insertIfMissing(userId, daily.getId(), WEDNESDAY);
        UserMission row = userMissionRepository.findForUpdate(userId, daily.getId(), WEDNESDAY).orElseThrow();
        row.advance();
        row.complete(Instant.parse("2026-11-25T10:00:00Z"), null);

        // When
        row.advance();

        // Then
        assertEquals(1, row.getProgress());
        assertTrue(row.isCompleted());
    }

    private UUID userId(String username) {
        return entityManager.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getSingleResult()
                .getId();
    }

    private Mission mission(String code) {
        return missionRepository.findByActiveTrue().stream()
                .filter(mission -> mission.getCode().equals(code))
                .findFirst()
                .orElseThrow();
    }
}
