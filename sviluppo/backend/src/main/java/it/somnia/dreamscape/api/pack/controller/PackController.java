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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.pack.dto.request.OpenPackRequest;
import it.somnia.dreamscape.api.pack.dto.response.OpenPackResponse;
import it.somnia.dreamscape.api.pack.dto.response.PackResponse;
import it.somnia.dreamscape.api.pack.service.PackService;
import lombok.RequiredArgsConstructor;

// Negozio dei pacchetti, solo per chi ha fatto l'accesso
@Tag(name = "Negozio", description = "I pacchetti in vendita, l'acquisto con apertura e la rilettura di "
        + "un'apertura già fatta.")
@RestController
@RequestMapping("/api/v1/packs")
@RequiredArgsConstructor
public class PackController {

    private final PackService packService;

    @Operation(summary = "Pacchetti in vendita", description = "Solo quelli in vendita in questo momento, dal più "
            + "economico, con le probabilità di ogni slot già in percentuale.")
    @ApiResponse(responseCode = "200", description = "I pacchetti")
    @GetMapping
    public List<PackResponse> listPacks() {
        return packService.listPacksOnSale();
    }

    // Il corpo è facoltativo: serve solo per scegliere la copertina
    @Operation(summary = "Acquisto e apertura", description = "Scala i crediti ed estrae le carte, che arrivano "
            + "subito nella collezione. Il corpo è facoltativo: serve solo per scegliere la copertina (coverId).")
    @ApiResponse(responseCode = "201", description = "Apertura fatta: id dell'apertura, saldo e carte")
    @ApiResponse(responseCode = "400", description = "INVALID_COVER: copertina non di questo pacchetto o spenta")
    @ApiResponse(responseCode = "404", description = "PACK_NOT_FOUND: non esiste o non si compra")
    @ApiResponse(responseCode = "409", description = "NOT_ENOUGH_CREDITS o PACK_NOT_ON_SALE")
    @PostMapping("/{packId}/open")
    public ResponseEntity<OpenPackResponse> openPack(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID packId,
            @RequestBody(required = false) OpenPackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packService.openPack(CurrentUser.id(jwt), packId, request));
    }
}
