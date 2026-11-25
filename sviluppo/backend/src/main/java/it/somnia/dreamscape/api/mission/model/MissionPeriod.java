package it.somnia.dreamscape.api.mission.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

// Ogni quanto si azzera una missione (decisione 015). Il giorno inizia a mezzanotte, ora italiana; la settimana
// il lunedì a mezzanotte. I giorni arrivano già nel fuso del gioco
public enum MissionPeriod {
    DAILY,
    WEEKLY;

    // Il primo giorno del periodo che contiene il giorno dato: il giorno stesso o il lunedì della sua settimana
    public LocalDate startOf(LocalDate day) {
        return this == DAILY ? day : day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // Il primo giorno del periodo dopo, cioè quando la missione si azzera
    public LocalDate nextStart(LocalDate periodStart) {
        return this == DAILY ? periodStart.plusDays(1) : periodStart.plusWeeks(1);
    }
}
