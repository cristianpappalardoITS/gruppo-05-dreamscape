package it.somnia.dreamscape.api.pack.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.pack.dto.response.PackOpeningResponse;
import it.somnia.dreamscape.api.pack.service.PackService;
import lombok.RequiredArgsConstructor;

// Le aperture dei pacchetti dell'utente collegato: la pagina dell'apertura le rilegge con l'id nell'URL
@RestController
@RequestMapping("/api/v1/pack-openings")
@RequiredArgsConstructor
public class PackOpeningController {

    private final PackService packService;

    @GetMapping("/{openingId}")
    public PackOpeningResponse getOpening(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID openingId) {
        return packService.getOpening(CurrentUser.id(jwt), openingId);
    }
}
