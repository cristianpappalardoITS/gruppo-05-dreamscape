package it.somnia.dreamscape.api.pack.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
