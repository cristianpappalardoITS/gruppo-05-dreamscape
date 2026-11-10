package it.somnia.dreamscape.api.pack.dto.response;

import java.util.UUID;

import it.somnia.dreamscape.api.pack.model.PackCover;

// Una copertina che il giocatore può scegliere: { "id", "name", "factionCode", "imageUrl" }
public record PackCoverResponse(UUID id, String name, String factionCode, String imageUrl) {

    public static PackCoverResponse from(PackCover cover) {
        String factionCode = cover.getFaction() != null ? cover.getFaction().getCode() : null;
        return new PackCoverResponse(cover.getId(), cover.getName(), factionCode, cover.getImageUrl());
    }
}
