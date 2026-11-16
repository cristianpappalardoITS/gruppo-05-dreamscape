package it.somnia.dreamscape.api.mission.dto.response;

import java.time.Instant;

// Una missione con il suo avanzamento, come la mostra la Dashboard (GET /me/summary).
// resetsAt è in UTC: la giornaliera si azzera a mezzanotte, ora italiana, la settimanale il lunedì.
public record MissionResponse(String code, String name, String description, int progress, int goalCount,
        int rewardCredits, boolean isCompleted, Instant resetsAt) {
}
