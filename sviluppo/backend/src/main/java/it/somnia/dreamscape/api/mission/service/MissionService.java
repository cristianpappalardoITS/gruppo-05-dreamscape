package it.somnia.dreamscape.api.mission.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.mission.dto.response.MissionResponse;
import it.somnia.dreamscape.api.mission.model.Mission;
import it.somnia.dreamscape.api.mission.model.UserMission;
import it.somnia.dreamscape.api.mission.repository.MissionRepository;
import it.somnia.dreamscape.api.mission.repository.UserMissionRepository;
import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.service.CreditService;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Missione giornaliera e obiettivo settimanale (#125, decisione 015).
 *
 * <p>Dopo ogni partita avanzano le missioni di chi ha scelto di giocare: chi avvia una partita rapida o
 * classificata e tutti e due i giocatori di una sfida tra amici. Chi viene scelto dal matchmaking non avanza,
 * perché non ha giocato. Quali partite contano lo dice {@code missions.match_type}: oggi la giornaliera conta ogni
 * partita, la settimanale solo le vittorie classificate (V105).
 *
 * <p>Il premio ({@code MISSION_REWARD}) arriva una volta per periodo, quando l'avanzamento raggiunge l'obiettivo.
 * Il giorno inizia a mezzanotte, ora italiana, la settimana il lunedì.
 */
@Service
@RequiredArgsConstructor
public class MissionService {

    // Prima le giornaliere, poi le settimanali; a parità per codice
    private static final Comparator<Mission> DASHBOARD_ORDER = Comparator.comparing(Mission::getPeriod)
            .thenComparing(Mission::getCode);

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserService userService;
    private final CreditService creditService;
    private final GameProperties gameProperties;
    private final Clock clock;

    /**
     * Fa avanzare le missioni dei giocatori della partita appena giocata e dà i premi delle missioni completate.
     * Va chiamato nella stessa transazione che salva la partita: se qualcosa va storto non si salva niente.
     *
     * @param match la partita appena salvata
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordMatch(Match match) {
        List<Mission> missions = missionRepository.findByActiveTrue();
        if (missions.isEmpty()) {
            return;
        }
        LocalDate day = LocalDate.ofInstant(match.getPlayedAt(), zone());
        // Righe dei giocatori bloccate in ordine di id, come il matchmaking: il premio cambia il saldo
        List<User> players = playersWhoCount(match).stream()
                .sorted(Comparator.comparing(User::getId))
                .map(player -> userService.getActiveUserForUpdate(player.getId()))
                .toList();
        for (User player : players) {
            boolean won = match.getWinner() != null && match.getWinner().getId().equals(player.getId());
            for (Mission mission : missions) {
                if (mission.countsMatch(match.getMatchType(), won)) {
                    advance(player, mission, mission.getPeriod().startOf(day), match);
                }
            }
        }
    }

    /**
     * Restituisce le missioni attive con l'avanzamento del giocatore nel periodo in corso, per la Dashboard
     * ({@code GET /me/summary}). Senza partite nel periodo l'avanzamento è 0.
     *
     * @param userId l'utente collegato
     * @return prima le giornaliere, poi le settimanali
     */
    @Transactional(readOnly = true)
    public List<MissionResponse> missionsOf(UUID userId) {
        LocalDate today = LocalDate.ofInstant(Instant.now(clock), zone());
        List<Mission> missions = missionRepository.findByActiveTrue().stream().sorted(DASHBOARD_ORDER).toList();
        Set<LocalDate> periodStarts = missions.stream()
                .map(mission -> mission.getPeriod().startOf(today))
                .collect(Collectors.toSet());
        Map<UUID, UserMission> currentRows = userMissionRepository.findByUserAndPeriodStarts(userId, periodStarts)
                .stream()
                // Una giornaliera di lunedì ha lo stesso giorno di inizio della settimanale: si tiene solo la
                // riga del periodo in corso di ogni missione
                .filter(row -> row.getPeriodStart().equals(row.getMission().getPeriod().startOf(today)))
                .collect(Collectors.toMap(row -> row.getMission().getId(), Function.identity()));
        return missions.stream()
                .map(mission -> toResponse(mission, currentRows.get(mission.getId()), today))
                .toList();
    }

    // Chi ha scelto di giocare: tutti e due in una sfida tra amici, solo chi la avvia nel matchmaking
    private static List<User> playersWhoCount(Match match) {
        return match.getChallenge() != null
                ? List.of(match.getChallenger(), match.getOpponent())
                : List.of(match.getChallenger());
    }

    private void advance(User lockedPlayer, Mission mission, LocalDate periodStart, Match match) {
        userMissionRepository.insertIfMissing(lockedPlayer.getId(), mission.getId(), periodStart);
        UserMission row = userMissionRepository.findForUpdate(lockedPlayer.getId(), mission.getId(), periodStart)
                .orElseThrow();
        if (row.isCompleted()) {
            return;
        }
        row.advance();
        if (row.getProgress() >= mission.getGoalCount()) {
            CreditTransaction reward = mission.getRewardCredits() > 0
                    ? creditService.apply(lockedPlayer, CreditTransactionType.MISSION_REWARD,
                            mission.getRewardCredits(), null, match.getId())
                    : null;
            row.complete(Instant.now(clock), reward);
        }
    }

    private MissionResponse toResponse(Mission mission, UserMission row, LocalDate today) {
        LocalDate periodStart = mission.getPeriod().startOf(today);
        Instant resetsAt = mission.getPeriod().nextStart(periodStart).atStartOfDay(zone()).toInstant();
        int progress = row == null ? 0 : Math.min(row.getProgress(), mission.getGoalCount());
        boolean completed = row != null && row.isCompleted();
        return new MissionResponse(mission.getCode(), mission.getName(), mission.getDescription(), progress,
                mission.getGoalCount(), mission.getRewardCredits(), completed, resetsAt);
    }

    private ZoneId zone() {
        return gameProperties.timeZone();
    }
}
