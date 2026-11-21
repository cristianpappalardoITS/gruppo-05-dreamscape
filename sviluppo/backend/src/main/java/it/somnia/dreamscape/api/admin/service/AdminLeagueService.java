package it.somnia.dreamscape.api.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.admin.dto.request.AdminLeaguesRequest;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.InvalidAdminFieldException;
import it.somnia.dreamscape.api.league.dto.response.LeagueResponse;
import it.somnia.dreamscape.api.league.model.League;
import it.somnia.dreamscape.api.league.repository.LeagueRepository;
import lombok.RequiredArgsConstructor;

/**
 * Soglie e premi delle leghe nel backoffice ({@code /admin/leagues}, decisione 011).
 *
 * <p>L'ordine delle leghe non cambia: la più bassa parte da 0, è quella di partenza e non ha premio; ogni lega ha
 * la soglia più alta della precedente. Le leghe già raggiunte restano tali: chi è sopra una soglia abbassata entra
 * nella lega, con il premio, alla sua prossima partita classificata.
 */
@Service
@RequiredArgsConstructor
public class AdminLeagueService {

    private final LeagueRepository leagueRepository;

    /**
     * Cambia soglie e premi di tutte le leghe insieme.
     *
     * @param request tutte le leghe, una volta ciascuna, con soglia e premio
     * @return le leghe aggiornate, dalla più bassa
     * @throws InvalidAdminFieldException se manca una lega o ce n'è una che non esiste, se la lega più bassa non
     *                                    parte da 0 o ha un premio, o se le soglie non salgono da una lega
     *                                    all'altra
     */
    @Transactional
    public List<LeagueResponse> updateLeagues(AdminLeaguesRequest request) {
        List<League> leagues = leagueRepository.findAllByOrderByMinRatingAsc();
        Map<String, Integer> positions = positionsByCode(request, leagues);
        if (positions.size() != leagues.size()) {
            throw new InvalidAdminFieldException("leagues", "Manda tutte le " + leagues.size() + " leghe.");
        }
        List<AdminLeaguesRequest.League> changes = leagues.stream()
                .map(league -> request.leagues().get(positions.get(league.getCode())))
                .toList();
        checkThresholds(changes, positions);
        applyThresholds(leagues, changes);
        for (int i = 0; i < leagues.size(); i++) {
            leagues.get(i).setRewardCredits(changes.get(i).rewardCredits());
        }
        return leagues.stream().map(LeagueResponse::from).toList();
    }

    // Per ogni codice la sua posizione nella richiesta, per indicare il campo sbagliato
    private static Map<String, Integer> positionsByCode(AdminLeaguesRequest request, List<League> leagues) {
        List<String> codes = leagues.stream().map(League::getCode).toList();
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < request.leagues().size(); i++) {
            String code = request.leagues().get(i).code();
            String field = "leagues[" + i + "].code";
            if (!codes.contains(code)) {
                throw new InvalidAdminFieldException(field, "La lega " + code + " non esiste.");
            }
            if (positions.putIfAbsent(code, i) != null) {
                throw new InvalidAdminFieldException(field, "La lega " + code + " è ripetuta.");
            }
        }
        return positions;
    }

    // changes è nell'ordine delle leghe, dalla più bassa
    private static void checkThresholds(List<AdminLeaguesRequest.League> changes, Map<String, Integer> positions) {
        AdminLeaguesRequest.League lowest = changes.getFirst();
        if (lowest.minRating() != 0) {
            throw new InvalidAdminFieldException(field(positions, lowest, "minRating"),
                    "La lega più bassa parte sempre da 0: è quella in cui si parte.");
        }
        if (lowest.rewardCredits() != 0) {
            throw new InvalidAdminFieldException(field(positions, lowest, "rewardCredits"),
                    "La lega più bassa è quella di partenza: non ha premio.");
        }
        for (int i = 1; i < changes.size(); i++) {
            AdminLeaguesRequest.League league = changes.get(i);
            if (league.minRating() <= changes.get(i - 1).minRating()) {
                throw new InvalidAdminFieldException(field(positions, league, "minRating"),
                        "Ogni lega deve partire da un punteggio più alto della lega prima.");
            }
        }
    }

    private static String field(Map<String, Integer> positions, AdminLeaguesRequest.League league, String name) {
        return "leagues[" + positions.get(league.code()) + "]." + name;
    }

    // min_rating è UNIQUE e PostgreSQL lo controlla a ogni riga aggiornata: prima le soglie che scendono, dalla
    // lega più bassa, poi quelle che salgono, dalla più alta. Visto che l'ordine delle leghe non cambia, così due
    // leghe non hanno mai la stessa soglia, nemmeno per un momento
    private void applyThresholds(List<League> leagues, List<AdminLeaguesRequest.League> changes) {
        for (int i = 0; i < leagues.size(); i++) {
            if (changes.get(i).minRating() < leagues.get(i).getMinRating()) {
                leagues.get(i).setMinRating(changes.get(i).minRating());
                leagueRepository.flush();
            }
        }
        for (int i = leagues.size() - 1; i >= 0; i--) {
            if (changes.get(i).minRating() > leagues.get(i).getMinRating()) {
                leagues.get(i).setMinRating(changes.get(i).minRating());
                leagueRepository.flush();
            }
        }
    }
}
