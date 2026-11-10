package it.somnia.dreamscape.api.pack.dto.response;

import java.util.Comparator;
import java.util.List;

import it.somnia.dreamscape.api.pack.model.PackSlot;
import it.somnia.dreamscape.api.pack.model.PackSlotRarity;

// Uno slot del pacchetto: quante carte dà e le rarità possibili, dalla più comune alla più rara
public record PackSlotResponse(int cardCount, List<PackSlotRarityResponse> rarities) {

    public static PackSlotResponse from(PackSlot slot) {
        int totalWeight = slot.getRarities().stream().mapToInt(PackSlotRarity::getWeight).sum();
        List<PackSlotRarityResponse> rarities = slot.getRarities().stream()
                .sorted(Comparator.comparingInt(slotRarity -> slotRarity.getRarity().getSortOrder()))
                .map(slotRarity -> new PackSlotRarityResponse(slotRarity.getRarity().getCode(),
                        chance(slotRarity.getWeight(), totalWeight)))
                .toList();
        return new PackSlotResponse(slot.getCardCount(), rarities);
    }

    // Percentuale con due decimali: con i pesi 70, 25 e 5 dà 70.0, 25.0 e 5.0
    private static double chance(int weight, int totalWeight) {
        return Math.round(weight * 10_000.0 / totalWeight) / 100.0;
    }
}
