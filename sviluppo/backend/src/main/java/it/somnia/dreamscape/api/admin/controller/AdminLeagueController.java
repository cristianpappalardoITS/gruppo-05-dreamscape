package it.somnia.dreamscape.api.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.admin.dto.request.AdminLeaguesRequest;
import it.somnia.dreamscape.api.admin.service.AdminLeagueService;
import it.somnia.dreamscape.api.league.dto.response.LeagueResponse;
import it.somnia.dreamscape.api.league.service.LeagueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Backoffice: soglie e premi delle leghe (solo ADMIN, SecurityConfig)
@Tag(name = "Backoffice: leghe", description = "Solo role ADMIN. Soglie e premi delle leghe del rank. Codici, "
        + "nomi e ordine delle leghe non cambiano.")
@RestController
@RequestMapping("/api/v1/admin/leagues")
@RequiredArgsConstructor
public class AdminLeagueController {

    private final AdminLeagueService adminLeagueService;
    private final LeagueService leagueService;

    @Operation(summary = "Le leghe", description = "Dalla più bassa, con soglia e premio: le stesse di GET /leagues.")
    @ApiResponse(responseCode = "200", description = "Le leghe")
    @GetMapping
    public List<LeagueResponse> listLeagues() {
        return leagueService.listLeagues();
    }

    @Operation(summary = "Modifica di soglie e premi", description = "Sempre tutte le leghe insieme. La più bassa "
            + "parte da 0 e non ha premio; ogni lega parte da un punteggio più alto della precedente. Le leghe già "
            + "raggiunte restano tali: chi è sopra una soglia abbassata ci entra alla prossima classificata.")
    @ApiResponse(responseCode = "200", description = "Le leghe aggiornate")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: una lega che manca, ripetuta o che non "
            + "esiste, la più bassa che non parte da 0 o con un premio, soglie che non salgono")
    @PutMapping
    public List<LeagueResponse> updateLeagues(@Valid @RequestBody AdminLeaguesRequest request) {
        return adminLeagueService.updateLeagues(request);
    }
}
