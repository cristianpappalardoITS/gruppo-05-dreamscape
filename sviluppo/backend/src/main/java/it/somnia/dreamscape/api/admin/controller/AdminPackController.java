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

import it.somnia.dreamscape.api.admin.dto.request.AdminPackRequest;
import it.somnia.dreamscape.api.admin.dto.response.AdminPackResponse;
import it.somnia.dreamscape.api.admin.service.AdminPackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Backoffice: i pacchetti (solo ADMIN, SecurityConfig). I pacchetti non si cancellano: si spengono
@RestController
@RequestMapping("/api/v1/admin/packs")
@RequiredArgsConstructor
public class AdminPackController {

    private final AdminPackService adminPackService;

    @GetMapping
    public List<AdminPackResponse> listPacks() {
        return adminPackService.listPacks();
    }

    @GetMapping("/{packId}")
    public AdminPackResponse getPack(@PathVariable UUID packId) {
        return adminPackService.getPack(packId);
    }

    @PostMapping
    public ResponseEntity<AdminPackResponse> createPack(@Valid @RequestBody AdminPackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminPackService.createPack(request));
    }

    @PutMapping("/{packId}")
    public AdminPackResponse updatePack(@PathVariable UUID packId, @Valid @RequestBody AdminPackRequest request) {
        return adminPackService.updatePack(packId, request);
    }
}
