package it.somnia.dreamscape.api.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.admin.dto.request.AdminFactionRequest;
import it.somnia.dreamscape.api.admin.dto.request.AdminGameModeRequest;
import it.somnia.dreamscape.api.admin.dto.request.AdminRarityRequest;
import it.somnia.dreamscape.api.admin.dto.response.AdminCardSetResponse;
import it.somnia.dreamscape.api.admin.dto.response.AdminEffectResponse;
import it.somnia.dreamscape.api.admin.dto.response.AdminFactionResponse;
import it.somnia.dreamscape.api.admin.dto.response.AdminGameModeResponse;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.ConfigNotFoundException;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.FactionNameTakenException;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.InvalidAdminFieldException;
import it.somnia.dreamscape.api.card.dto.response.RarityRuleResponse;
import it.somnia.dreamscape.api.card.model.Faction;
import it.somnia.dreamscape.api.card.model.FactionAdvantage;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.card.repository.CardSetRepository;
import it.somnia.dreamscape.api.card.repository.EffectRepository;
import it.somnia.dreamscape.api.card.repository.FactionAdvantageRepository;
import it.somnia.dreamscape.api.card.repository.FactionRepository;
import it.somnia.dreamscape.api.card.repository.RarityRepository;
import it.somnia.dreamscape.api.match.model.GameMode;
import it.somnia.dreamscape.api.match.repository.GameModeRepository;
import lombok.RequiredArgsConstructor;

/**
 * La configurazione del gioco nel backoffice: fazioni con i loro vantaggi, limiti delle rarità nei mazzi e valori
 * delle modalità (#95). Soglie e premi delle leghe sono in {@link AdminLeagueService}.
 *
 * <p>Codici, ordine delle rarità e carta Lord delle fazioni non cambiano. I nuovi valori valgono da subito:
 * i mazzi si ricontrollano a ogni lettura e prima di ogni partita, quindi un mazzo che non rispetta più i limiti
 * delle rarità lo dice nei suoi {@code problems}; le partite già giocate hanno la copia dei loro valori.
 */
@Service
@RequiredArgsConstructor
public class AdminConfigService {

    private final FactionRepository factionRepository;
    private final FactionAdvantageRepository factionAdvantageRepository;
    private final RarityRepository rarityRepository;
    private final GameModeRepository gameModeRepository;
    private final EffectRepository effectRepository;
    private final CardSetRepository cardSetRepository;

    /**
     * Restituisce gli effetti accesi, che si possono assegnare alle carte, in ordine di nome: servono al form
     * delle carte del backoffice.
     *
     * @return gli effetti
     */
    @Transactional(readOnly = true)
    public List<AdminEffectResponse> listEffects() {
        return effectRepository.findByActiveTrueOrderByNameAsc().stream().map(AdminEffectResponse::from).toList();
    }

    /**
     * Restituisce le espansioni delle carte, in ordine di codice.
     *
     * @return le espansioni
     */
    @Transactional(readOnly = true)
    public List<AdminCardSetResponse> listCardSets() {
        return cardSetRepository.findAll().stream()
                .sorted((first, second) -> first.getCode().compareTo(second.getCode()))
                .map(AdminCardSetResponse::from)
                .toList();
    }

    /**
     * Restituisce le fazioni con i loro vantaggi, in ordine di codice.
     *
     * @return le fazioni
     */
    @Transactional(readOnly = true)
    public List<AdminFactionResponse> listFactions() {
        return factionRepository.findAllByOrderByCodeAsc().stream()
                .map(faction -> AdminFactionResponse.from(faction, factionAdvantageRepository.findByFaction(faction)))
                .toList();
    }

    /**
     * Cambia nome, descrizione, titolo del Lord e vantaggi di una fazione. I vantaggi mandati sostituiscono quelli
     * di prima.
     *
     * @param code    il codice della fazione
     * @param request i nuovi valori
     * @return la fazione aggiornata
     * @throws ConfigNotFoundException     se la fazione non esiste
     * @throws FactionNameTakenException   se un'altra fazione ha già questo nome
     * @throws InvalidAdminFieldException  se un vantaggio vale contro la fazione stessa, contro una fazione che non
     *                                     esiste o ripetuta, o non ha nessun bonus
     */
    @Transactional
    public AdminFactionResponse updateFaction(String code, AdminFactionRequest request) {
        Faction faction = factionRepository.findByCode(code).orElseThrow(() -> new ConfigNotFoundException(
                "FACTION_NOT_FOUND", "Fazione non trovata."));
        String name = request.name().trim();
        if (factionRepository.existsByNameIgnoreCaseAndIdNot(name, faction.getId())) {
            throw new FactionNameTakenException();
        }
        Map<UUID, WantedAdvantage> wanted = wantedAdvantages(faction, request.advantages());
        faction.setName(name);
        faction.setDescription(blankToNull(request.description()));
        faction.setLordTitle(blankToNull(request.lordTitle()));
        replaceAdvantages(faction, wanted);
        return AdminFactionResponse.from(faction, factionAdvantageRepository.findByFaction(faction));
    }

    /**
     * Restituisce le rarità con i limiti nei mazzi, dalla Comune alla Leggendaria.
     *
     * @return le rarità
     */
    @Transactional(readOnly = true)
    public List<RarityRuleResponse> listRarities() {
        return rarityRepository.findAllByOrderBySortOrderAsc().stream().map(RarityRuleResponse::from).toList();
    }

    /**
     * Cambia nome e limiti di una rarità. I mazzi salvati che non rispettano più i limiti diventano non validi.
     *
     * @param code    il codice della rarità
     * @param request i nuovi valori
     * @return la rarità aggiornata
     * @throws ConfigNotFoundException se la rarità non esiste
     */
    @Transactional
    public RarityRuleResponse updateRarity(String code, AdminRarityRequest request) {
        Rarity rarity = rarityRepository.findByCode(code).orElseThrow(() -> new ConfigNotFoundException(
                "RARITY_NOT_FOUND", "Rarità non trovata."));
        rarity.setName(request.name().trim());
        rarity.setMaxPerDeck(request.maxPerDeck());
        rarity.setMaxCopies(request.maxCopies());
        return RarityRuleResponse.from(rarity);
    }

    /**
     * Restituisce le modalità di gioco. Oggi c'è solo BASE.
     *
     * @return le modalità
     */
    @Transactional(readOnly = true)
    public List<AdminGameModeResponse> listGameModes() {
        return gameModeRepository.findAll().stream()
                .sorted((first, second) -> first.getCode().compareTo(second.getCode()))
                .map(AdminGameModeResponse::from)
                .toList();
    }

    /**
     * Cambia nome, descrizione, punti vita iniziali e limite di round di una modalità. Valgono per le partite
     * giocate da adesso.
     *
     * @param code    il codice della modalità
     * @param request i nuovi valori
     * @return la modalità aggiornata
     * @throws ConfigNotFoundException se la modalità non esiste
     */
    @Transactional
    public AdminGameModeResponse updateGameMode(String code, AdminGameModeRequest request) {
        GameMode mode = gameModeRepository.findByCode(code).orElseThrow(() -> new ConfigNotFoundException(
                "GAME_MODE_NOT_FOUND", "Modalità non trovata."));
        mode.setName(request.name().trim());
        mode.setDescription(blankToNull(request.description()));
        mode.setStartingLifePoints(request.startingLifePoints());
        mode.setRoundLimit(request.roundLimit());
        return AdminGameModeResponse.from(mode);
    }

    // I vantaggi della richiesta per id della fazione bersaglio, dopo i controlli (chk_faction_advantages_*)
    private Map<UUID, WantedAdvantage> wantedAdvantages(Faction faction,
            List<AdminFactionRequest.Advantage> advantages) {
        Map<UUID, WantedAdvantage> wanted = new HashMap<>();
        for (int i = 0; i < advantages.size(); i++) {
            AdminFactionRequest.Advantage advantage = advantages.get(i);
            String field = "advantages[" + i + "]";
            Faction target = factionRepository.findByCode(advantage.targetFaction())
                    .orElseThrow(() -> new InvalidAdminFieldException(field + ".targetFaction",
                            "La fazione " + advantage.targetFaction() + " non esiste."));
            if (target.getId().equals(faction.getId())) {
                throw new InvalidAdminFieldException(field + ".targetFaction",
                        "Una fazione non può avere un vantaggio contro se stessa.");
            }
            if (wanted.containsKey(target.getId())) {
                throw new InvalidAdminFieldException(field + ".targetFaction",
                        "Il vantaggio contro " + target.getName() + " è ripetuto.");
            }
            if (advantage.attackBonus() == 0 && advantage.defenseBonus() == 0) {
                throw new InvalidAdminFieldException(field + ".attackBonus",
                        "Un vantaggio ha almeno un bonus: per toglierlo, non mandarlo.");
            }
            wanted.put(target.getId(), new WantedAdvantage(target, advantage.attackBonus(),
                    advantage.defenseBonus()));
        }
        return wanted;
    }

    // Si aggiornano le righe che restano, si tolgono quelle che mancano e si aggiungono le nuove: così la coppia
    // (fazione, bersaglio) di uq_faction_advantages_pair non compare mai due volte
    private void replaceAdvantages(Faction faction, Map<UUID, WantedAdvantage> wanted) {
        Map<UUID, WantedAdvantage> toAdd = new HashMap<>(wanted);
        for (FactionAdvantage existing : factionAdvantageRepository.findByFaction(faction)) {
            WantedAdvantage change = toAdd.remove(existing.getTargetFaction().getId());
            if (change == null) {
                factionAdvantageRepository.delete(existing);
            } else {
                existing.setAttackBonus(change.attackBonus());
                existing.setDefenseBonus(change.defenseBonus());
            }
        }
        toAdd.values().forEach(advantage -> factionAdvantageRepository.save(new FactionAdvantage(faction,
                advantage.target(), advantage.attackBonus(), advantage.defenseBonus())));
        factionAdvantageRepository.flush();
    }

    private record WantedAdvantage(Faction target, int attackBonus, int defenseBonus) {
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
