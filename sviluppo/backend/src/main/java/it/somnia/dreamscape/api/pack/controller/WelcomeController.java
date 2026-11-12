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

import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.pack.dto.request.WelcomeRequest;
import it.somnia.dreamscape.api.pack.dto.response.WelcomeResponse;
import it.somnia.dreamscape.api.pack.service.WelcomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Pagina Benvenuto: scelta della fazione, set iniziale e pacchetti di benvenuto. Sta in pack/ perché
// i regali sono pacchetti aperti (guida SWD, struttura del backend).
@RestController
@RequestMapping("/api/v1/me/welcome")
@RequiredArgsConstructor
public class WelcomeController {

    private final WelcomeService welcomeService;

    @GetMapping
    public WelcomeResponse getWelcome(@AuthenticationPrincipal Jwt jwt) {
        return welcomeService.getWelcome(CurrentUser.id(jwt));
    }

    @PostMapping
    public ResponseEntity<WelcomeResponse> claimWelcome(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WelcomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(welcomeService.claimWelcome(CurrentUser.id(jwt), request));
    }
}
