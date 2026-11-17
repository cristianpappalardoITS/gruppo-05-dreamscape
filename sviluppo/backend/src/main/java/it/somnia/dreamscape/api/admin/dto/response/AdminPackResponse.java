package it.somnia.dreamscape.api.admin.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackCover;
import it.somnia.dreamscape.api.pack.model.PackType;

// Un pacchetto come lo vede il backoffice, compresi i pacchetti spenti e le copertine spente.
// I pesi degli slot sono quelli salvati (percentuali), non le probabilità calcolate del negozio.
public record AdminPackResponse(
        UUID id,
        String name,
        String description,
        PackType packType,
        int priceCredits,
        String cardSetCode,
        String factionCode,
        BigDecimal fullArtChance,
        Instant availableFrom,
        Instant availableUntil,
        String imageUrl,
        boolean isActive,
        List<Slot> slots,
        List<Cover> covers) {

    public static AdminPackResponse from(Pack pack, List<PackCover> covers) {
        List<Slot> slots = pack.getSlots().stream()
                .map(slot -> new Slot(slot.getCardCount(), slot.getRarities().stream()
                        .sorted((a, b) -> Integer.compare(a.getRarity().getSortOrder(), b.getRarity().getSortOrder()))
                        .map(rarity -> new SlotRarity(rarity.getRarity().getCode(), rarity.getWeight()))
                        .toList()))
                .toList();
        return new AdminPackResponse(pack.getId(), pack.getName(), pack.getDescription(), pack.getPackType(),
                pack.getPriceCredits(), pack.getCardSet() != null ? pack.getCardSet().getCode() : null,
                pack.getFaction() != null ? pack.getFaction().getCode() : null, pack.getFullArtChance(),
                pack.getAvailableFrom(), pack.getAvailableUntil(), pack.getImageUrl(), pack.isActive(), slots,
                covers.stream().map(Cover::from).toList());
    }

    public record Slot(int cardCount, List<SlotRarity> rarities) {
    }

    public record SlotRarity(String rarityCode, int weight) {
    }

    public record Cover(UUID id, String name, String factionCode, String imageUrl, boolean isActive) {

        static Cover from(PackCover cover) {
            return new Cover(cover.getId(), cover.getName(),
                    cover.getFaction() != null ? cover.getFaction().getCode() : null, cover.getImageUrl(),
                    cover.isActive());
        }
    }
}
