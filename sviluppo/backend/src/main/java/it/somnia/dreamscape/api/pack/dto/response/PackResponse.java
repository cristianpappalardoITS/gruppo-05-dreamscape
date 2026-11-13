package it.somnia.dreamscape.api.pack.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackCover;

// Un pacchetto in vendita nel negozio (guida ai dati, "Negozio e apertura dei pacchetti").
// factionCode non è null se il pacchetto ha carte di una sola fazione; availableUntil (UTC) se è a tempo.
// imageUrl è l'immagine del pacchetto (decisione 030), null finché non c'è: il negozio ne mostra una generica.
public record PackResponse(
        UUID id,
        String name,
        String description,
        int priceCredits,
        BigDecimal fullArtChance,
        String factionCode,
        Instant availableUntil,
        String imageUrl,
        List<PackCoverResponse> covers,
        List<PackSlotResponse> slots) {

    public static PackResponse from(Pack pack) {
        String factionCode = pack.getFaction() != null ? pack.getFaction().getCode() : null;
        List<PackCoverResponse> covers = pack.getCovers().stream()
                .filter(PackCover::isActive)
                .map(PackCoverResponse::from)
                .toList();
        List<PackSlotResponse> slots = pack.getSlots().stream().map(PackSlotResponse::from).toList();
        return new PackResponse(pack.getId(), pack.getName(), pack.getDescription(), pack.getPriceCredits(),
                pack.getFullArtChance(), factionCode, pack.getAvailableUntil(), pack.getImageUrl(), covers, slots);
    }
}
