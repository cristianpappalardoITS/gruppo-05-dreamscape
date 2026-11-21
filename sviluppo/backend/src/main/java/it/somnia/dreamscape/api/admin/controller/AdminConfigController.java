package it.somnia.dreamscape.api.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.admin.dto.request.AdminFactionRequest;
import it.somnia.dreamscape.api.admin.dto.request.AdminGameModeRequest;
import it.somnia.dreamscape.api.admin.dto.request.AdminRarityRequest;
import it.somnia.dreamscape.api.admin.dto.response.AdminFactionResponse;
import it.somnia.dreamscape.api.admin.dto.response.AdminGameModeResponse;
import it.somnia.dreamscape.api.admin.service.AdminConfigService;
import it.somnia.dreamscape.api.card.dto.response.RarityRuleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Backoffice: la configurazione del gioco (solo ADMIN, SecurityConfig). Le leghe sono in AdminLeagueController
@Tag(name = "Backoffice: configurazione", description = "Solo role ADMIN. Fazioni con i loro vantaggi, limiti "
        + "delle rarità nei mazzi, punti vita e limite di round della modalità. Codici, ordine e carta Lord non "
        + "cambiano; le carte per mazzo (20) sono in sola lettura (GET /rules).")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    @Operation(summary = "Le fazioni", description = "In ordine di codice, con la carta Lord e i vantaggi.")
    @ApiResponse(responseCode = "200", description = "Le fazioni")
    @GetMapping("/factions")
    public List<AdminFactionResponse> listFactions() {
        return adminConfigService.listFactions();
    }

    @Operation(summary = "Modifica di una fazione", description = "Nome, descrizione, titolo del Lord e vantaggi. "
            + "I vantaggi mandati sostituiscono quelli di prima: per toglierne uno non lo si manda. La carta Lord "
            + "non si cambia.")
    @ApiResponse(responseCode = "200", description = "La fazione aggiornata")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: vantaggio contro se stessa, contro una "
            + "fazione che non esiste, ripetuto o senza bonus, campi mancanti o troppo lunghi")
    @ApiResponse(responseCode = "404", description = "FACTION_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "FACTION_NAME_TAKEN")
    @PutMapping("/factions/{code}")
    public AdminFactionResponse updateFaction(@PathVariable String code,
            @Valid @RequestBody AdminFactionRequest request) {
        return adminConfigService.updateFaction(code, request);
    }

    @Operation(summary = "Le rarità", description = "Dalla Comune alla Leggendaria, con i limiti nei mazzi.")
    @ApiResponse(responseCode = "200", description = "Le rarità")
    @GetMapping("/rarities")
    public List<RarityRuleResponse> listRarities() {
        return adminConfigService.listRarities();
    }

    @Operation(summary = "Modifica di una rarità", description = "Nome, carte al massimo per mazzo (null = nessun "
            + "limite) e copie al massimo della stessa carta. Valgono da subito: i mazzi salvati che non li "
            + "rispettano più diventano non validi e lo dicono nei loro problems.")
    @ApiResponse(responseCode = "200", description = "La rarità aggiornata")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    @ApiResponse(responseCode = "404", description = "RARITY_NOT_FOUND")
    @PutMapping("/rarities/{code}")
    public RarityRuleResponse updateRarity(@PathVariable String code, @Valid @RequestBody AdminRarityRequest request) {
        return adminConfigService.updateRarity(code, request);
    }

    @Operation(summary = "Le modalità di gioco", description = "Oggi c'è solo BASE.")
    @ApiResponse(responseCode = "200", description = "Le modalità")
    @GetMapping("/game-modes")
    public List<AdminGameModeResponse> listGameModes() {
        return adminConfigService.listGameModes();
    }

    @Operation(summary = "Modifica di una modalità", description = "Nome, descrizione, punti vita iniziali e "
            + "limite di round. Valgono per le partite giocate da adesso: quelle già giocate tengono i loro valori.")
    @ApiResponse(responseCode = "200", description = "La modalità aggiornata")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    @ApiResponse(responseCode = "404", description = "GAME_MODE_NOT_FOUND")
    @PutMapping("/game-modes/{code}")
    public AdminGameModeResponse updateGameMode(@PathVariable String code,
            @Valid @RequestBody AdminGameModeRequest request) {
        return adminConfigService.updateGameMode(code, request);
    }
}
