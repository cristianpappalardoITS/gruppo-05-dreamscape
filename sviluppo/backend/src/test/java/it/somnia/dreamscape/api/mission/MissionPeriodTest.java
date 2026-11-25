package it.somnia.dreamscape.api.mission;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import it.somnia.dreamscape.api.mission.model.MissionPeriod;

// Inizio e fine dei periodi delle missioni (decisione 015): il giorno è quello, la settimana parte dal lunedì
class MissionPeriodTest {

    // Lunedì 23 novembre 2026
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

    @Test
    void startOf_daily_isTheSameDay() {
        assertThat(MissionPeriod.DAILY.startOf(MONDAY.plusDays(2))).isEqualTo(MONDAY.plusDays(2));
    }

    @Test
    void startOf_weeklyOnMonday_isThatMonday() {
        assertThat(MissionPeriod.WEEKLY.startOf(MONDAY)).isEqualTo(MONDAY);
    }

    @Test
    void startOf_weeklyOnSunday_isTheMondayBefore() {
        assertThat(MissionPeriod.WEEKLY.startOf(MONDAY.plusDays(6))).isEqualTo(MONDAY);
    }

    @Test
    void nextStart_daily_isTheNextDay() {
        assertThat(MissionPeriod.DAILY.nextStart(MONDAY)).isEqualTo(MONDAY.plusDays(1));
    }

    @Test
    void nextStart_weekly_isTheNextMonday() {
        assertThat(MissionPeriod.WEEKLY.nextStart(MONDAY)).isEqualTo(MONDAY.plusWeeks(1));
    }
}
