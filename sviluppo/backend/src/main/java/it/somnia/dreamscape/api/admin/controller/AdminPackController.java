package it.somnia.dreamscape.api.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.admin.dto.request.AdminPackRequest;
import it.somnia.dreamscape.api.admin.dto.response.AdminPackResponse;
import it.somnia.dreamscape.api.admin.service.AdminPackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Backoffice: i pacchetti (solo ADMIN, SecurityConfig). I pacchetti non si cancellano: si spengono
@Tag(name = "Backoffice: pacchetti", description = "Solo role ADMIN. Tutti i pacchetti, anche spenti, set "
        + "iniziali e benvenuto. I pacchetti non si cancellano: si spengono con isActive false.")
@RestController
@RequestMapping("/api/v1/admin/packs")
@RequiredArgsConstructor
public class AdminPackController {

    private final AdminPackService adminPackService;

    @Operation(summary = "Tutti i pacchetti", description = "Con slot, pesi e copertine.")
    @ApiResponse(responseCode = "200", description = "I pacchetti")
    @GetMapping
    public List<AdminPackResponse> listPacks() {
        return adminPackService.listPacks();
    }

    @Operation(summary = "Un pacchetto")
    @ApiResponse(responseCode = "200", description = "Il pacchetto")
    @ApiResponse(responseCode = "404", description = "PACK_NOT_FOUND")
    @GetMapping("/{packId}")
    public AdminPackResponse getPack(@PathVariable UUID packId) {
        return adminPackService.getPack(packId);
    }

    @Operation(summary = "Nuovo pacchetto", description = "SHOP con prezzo maggiore di 0, STARTER e WELCOME gratis. "
            + "Almeno uno slot; i pesi di ogni slot sono percentuali e sommano a 100.")
    @ApiResponse(responseCode = "201", description = "Pacchetto creato")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: pesi che non sommano a 100, prezzo "
            + "sbagliato, fine della vendita prima dell'inizio, rarità, fazione o espansione che non esistono")
    @ApiResponse(responseCode = "409", description = "PACK_NAME_TAKEN")
    @PostMapping
    public ResponseEntity<AdminPackResponse> createPack(@Valid @RequestBody AdminPackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminPackService.createPack(request));
    }

    @Operation(summary = "Modifica o spegnimento di un pacchetto", description = "Gli slot mandati sostituiscono "
            + "quelli di prima. Copertine: con id si modificano, senza id si aggiungono, quelle che mancano si "
            + "spengono.")
    @ApiResponse(responseCode = "200", description = "Pacchetto aggiornato")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, anche per una copertina di un altro pacchetto")
    @ApiResponse(responseCode = "404", description = "PACK_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "PACK_NAME_TAKEN")
    @PutMapping("/{packId}")
    public AdminPackResponse updatePack(@PathVariable UUID packId, @Valid @RequestBody AdminPackRequest request) {
        return adminPackService.updatePack(packId, request);
    }
}
