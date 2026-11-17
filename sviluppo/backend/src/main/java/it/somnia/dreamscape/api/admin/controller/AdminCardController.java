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
@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
public class AdminCardController {

    private final AdminCardService adminCardService;

    @GetMapping
    public PageResponse<AdminCardResponse> listCards(
            @RequestParam(required = false) String faction,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) @Size(max = 80) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean fullArt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return adminCardService.listCards(faction, rarity, search, active, fullArt, page, size);
    }

    @GetMapping("/{cardId}")
    public AdminCardResponse getCard(@PathVariable UUID cardId) {
        return adminCardService.getCard(cardId);
    }

    @PostMapping
    public ResponseEntity<AdminCardResponse> createCard(@Valid @RequestBody AdminCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCardService.createCard(request));
    }

    @PutMapping("/{cardId}")
    public AdminCardResponse updateCard(@PathVariable UUID cardId, @Valid @RequestBody AdminCardRequest request) {
        return adminCardService.updateCard(cardId, request);
    }
}
