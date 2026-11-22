package it.somnia.dreamscape.api.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.admin.dto.response.AdminStatsResponse;
import it.somnia.dreamscape.api.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;

// Backoffice: i numeri della Dashboard admin (solo ADMIN, SecurityConfig)
@Tag(name = "Backoffice: statistiche", description = "Solo role ADMIN. I numeri della Dashboard admin: giocatori, "
        + "carte, pacchetti, partite e Dream Credits.")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "I numeri della Dashboard admin", description = "Contano solo i giocatori, non gli admin. "
            + "Oggi e ultimi 7 giorni (oggi e i 6 giorni prima) in ora italiana, da mezzanotte. I crediti: la somma "
            + "dei saldi e i movimenti degli ultimi 7 giorni, entrati e usciti, anche per tipo.")
    @ApiResponse(responseCode = "200", description = "I numeri di adesso")
    @GetMapping
    public AdminStatsResponse getStats() {
        return adminStatsService.getStats();
    }
}
