package it.somnia.dreamscape.api.admin.dto.response;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import it.somnia.dreamscape.api.card.model.Faction;
import it.somnia.dreamscape.api.card.model.FactionAdvantage;

/**
 * Una fazione nel backoffice ({@code /admin/factions}).
 *
 * @param code        codice fisso, es. DREAMS
 * @param name        nome
 * @param description descrizione per la pagina "Il gioco"
 * @param lordTitle   titolo del Lord, es. "Dreams Lord"
 * @param lordCard    la carta Lord, che dal backoffice non si cambia; null se non c'è
 * @param advantages  contro quali fazioni è forte e con quale bonus
 */
public record AdminFactionResponse(String code, String name, String description, String lordTitle,
        LordCard lordCard, List<Advantage> advantages) {

    /**
     * La carta Lord della fazione.
     *
     * @param id   l'id della carta
     * @param code il codice, es. BASE-004
     * @param name il nome
     */
    public record LordCard(UUID id, String code, String name) {
    }

    /**
     * Un vantaggio della fazione.
     *
     * @param targetFaction la fazione contro cui vale
     * @param attackBonus   punti di attacco in più
     * @param defenseBonus  punti di difesa in più
     */
    public record Advantage(String targetFaction, int attackBonus, int defenseBonus) {
    }

    public static AdminFactionResponse from(Faction faction, List<FactionAdvantage> advantages) {
        LordCard lordCard = faction.getLordCard() == null ? null
                : new LordCard(faction.getLordCard().getId(), faction.getLordCard().getCode(),
                        faction.getLordCard().getName());
        List<Advantage> advantageResponses = advantages.stream()
                .map(advantage -> new Advantage(advantage.getTargetFaction().getCode(), advantage.getAttackBonus(),
                        advantage.getDefenseBonus()))
                .sorted(Comparator.comparing(Advantage::targetFaction))
                .toList();
        return new AdminFactionResponse(faction.getCode(), faction.getName(), faction.getDescription(),
                faction.getLordTitle(), lordCard, advantageResponses);
    }
}
