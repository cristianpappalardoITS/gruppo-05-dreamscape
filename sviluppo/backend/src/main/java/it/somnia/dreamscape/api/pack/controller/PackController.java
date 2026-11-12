package it.somnia.dreamscape.api.pack.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.pack.dto.request.OpenPackRequest;
import it.somnia.dreamscape.api.pack.dto.response.OpenPackResponse;
import it.somnia.dreamscape.api.pack.dto.response.PackResponse;
import it.somnia.dreamscape.api.pack.service.PackService;
import lombok.RequiredArgsConstructor;

// Negozio dei pacchetti, solo per chi ha fatto l'accesso
@RestController
@RequestMapping("/api/v1/packs")
@RequiredArgsConstructor
public class PackController {

    private final PackService packService;

    @GetMapping
    public List<PackResponse> listPacks() {
        return packService.listPacksOnSale();
    }

    // Il corpo è facoltativo: serve solo per scegliere la copertina
    @PostMapping("/{packId}/open")
    public ResponseEntity<OpenPackResponse> openPack(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID packId,
            @RequestBody(required = false) OpenPackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packService.openPack(CurrentUser.id(jwt), packId, request));
    }
}
