package it.somnia.dreamscape.api.pack.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.pack.dto.request.WelcomeRequest;
import it.somnia.dreamscape.api.pack.dto.response.WelcomeResponse;
import it.somnia.dreamscape.api.pack.service.WelcomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Pagina Benvenuto: scelta della fazione, set iniziale e pacchetti di benvenuto. Sta in pack/ perché
// i regali sono pacchetti aperti (guida SWD, struttura del backend).
@Tag(name = "Benvenuto", description = "Dopo la registrazione: scelta della fazione, set iniziale di 20 carte e "
        + "due pacchetti di benvenuto, gratis.")
@RestController
@RequestMapping("/api/v1/me/welcome")
@RequiredArgsConstructor
public class WelcomeController {

    private final WelcomeService welcomeService;

    @Operation(summary = "Stato del benvenuto", description = "Se starterFaction è null la fazione non è ancora "
            + "scelta e le liste sono vuote.")
    @ApiResponse(responseCode = "200", description = "Fazione scelta, set iniziale e aperture di benvenuto")
    @GetMapping
    public WelcomeResponse getWelcome(@AuthenticationPrincipal Jwt jwt) {
        return welcomeService.getWelcome(CurrentUser.id(jwt));
    }

    @Operation(summary = "Scelta della fazione", description = "Dà il set iniziale della fazione e apre i due "
            + "pacchetti di benvenuto. Si fa una volta sola.")
    @ApiResponse(responseCode = "201", description = "Regali consegnati, stessa forma di GET /me/welcome")
    @ApiResponse(responseCode = "400", description = "INVALID_FACTION (fazione che non esiste) o VALIDATION_ERROR "
            + "(fazione vuota)")
    @ApiResponse(responseCode = "409", description = "WELCOME_ALREADY_CLAIMED")
    @PostMapping
    public ResponseEntity<WelcomeResponse> claimWelcome(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WelcomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(welcomeService.claimWelcome(CurrentUser.id(jwt), request));
    }
}
