package it.somnia.dreamscape.api.league.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.league.dto.response.LeagueResponse;
import it.somnia.dreamscape.api.league.service.LeagueService;
import lombok.RequiredArgsConstructor;

// Le leghe del rank (pagina Classifica). La lega del giocatore e il suo punteggio sono in GET /me/summary
@Tag(name = "Leghe", description = "Le leghe del rank con soglie e premi. La lega del giocatore e il suo "
        + "punteggio sono in GET /me/summary.")
@RestController
@RequestMapping("/api/v1/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;

    @Operation(summary = "Le leghe", description = "Dalla più bassa alla più alta, con il punteggio da cui si entra "
            + "e il premio in Dream Credits della prima volta. La più bassa parte da 0 ed è quella di partenza.")
    @ApiResponse(responseCode = "200", description = "Le leghe")
    @GetMapping
    public List<LeagueResponse> listLeagues() {
        return leagueService.listLeagues();
    }
}
