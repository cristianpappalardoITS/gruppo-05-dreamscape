package it.somnia.dreamscape.api.admin.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.admin.dto.request.AdminCardRequest;
import it.somnia.dreamscape.api.admin.dto.response.AdminCardResponse;
import it.somnia.dreamscape.api.admin.service.AdminCardService;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// Backoffice: le carte del catalogo (solo ADMIN, SecurityConfig). Le carte non si cancellano: si disattivano
@Tag(name = "Backoffice: carte", description = "Solo role ADMIN. Tutte le carte, anche full art e disattivate. "
        + "Le carte non si cancellano: si disattivano con isActive false.")
@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
public class AdminCardController {

    private final AdminCardService adminCardService;

    @Operation(summary = "Tutte le carte", description = "In ordine di codice, con deckCount (in quanti mazzi è "
            + "usata). I filtri sono tutti facoltativi.")
    @ApiResponse(responseCode = "200", description = "Una pagina di carte")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: search troppo lungo, page o size fuori dai limiti")
    @GetMapping
    public PageResponse<AdminCardResponse> listCards(
            @Parameter(description = "Codice della fazione, es. DREAMS") @RequestParam(required = false) String faction,
            @Parameter(description = "Codice della rarità, es. LEGENDARY")
            @RequestParam(required = false) String rarity,
            @Parameter(description = "Parte del nome o del codice")
            @RequestParam(required = false) @Size(max = 80) String search,
            @Parameter(description = "true solo le attive, false solo le disattivate, senza il parametro tutte")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "true solo le full art, false solo le carte normali, senza il parametro tutte")
            @RequestParam(required = false) Boolean fullArt,
            @Parameter(description = "Numero della pagina, da 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Carte per pagina, da 1 a 100")
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return adminCardService.listCards(faction, rarity, search, active, fullArt, page, size);
    }

    @Operation(summary = "Una carta", description = "I dati salvati: una full art ha nome, statistiche ed effetto "
            + "vuoti.")
    @ApiResponse(responseCode = "200", description = "La carta")
    @ApiResponse(responseCode = "404", description = "CARD_NOT_FOUND")
    @GetMapping("/{cardId}")
    public AdminCardResponse getCard(@PathVariable UUID cardId) {
        return adminCardService.getCard(cardId);
    }

    @Operation(summary = "Nuova carta o full art", description = "Carta normale: code, name, attack, defense, "
            + "factionCode, rarityCode e cardSetCode. Full art: solo code (codice della carta base + -FA), "
            + "baseCardId, illustrazione e isActive; oggi solo per le Leggendarie, una per carta.")
    @ApiResponse(responseCode = "201", description = "Carta creata")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: campi mancanti, fazione o effetto che non "
            + "esistono, valore X sbagliato, full art di una carta non Leggendaria")
    @ApiResponse(responseCode = "409", description = "CARD_CODE_TAKEN, CARD_NAME_TAKEN o FULL_ART_EXISTS")
    @PostMapping
    public ResponseEntity<AdminCardResponse> createCard(@Valid @RequestBody AdminCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCardService.createCard(request));
    }

    @Operation(summary = "Modifica o disattivazione di una carta", description = "Si mandano tutti i dati della "
            + "carta: quelli mancanti si svuotano, tranne illustrazione e ritaglio, che restano come sono (le due URL "
            + "vuote tolgono l'illustrazione). Il codice e la carta base non si cambiano; il Lord non cambia fazione "
            + "e, come una carta con la full art, resta Leggendaria. Prima di disattivare conviene "
            + "guardare deckCount.")
    @ApiResponse(responseCode = "200", description = "Carta aggiornata")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, anche per un codice o una carta base diversi")
    @ApiResponse(responseCode = "404", description = "CARD_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CARD_NAME_TAKEN")
    @PutMapping("/{cardId}")
    public AdminCardResponse updateCard(@PathVariable UUID cardId, @Valid @RequestBody AdminCardRequest request) {
        return adminCardService.updateCard(cardId, request);
    }
}
