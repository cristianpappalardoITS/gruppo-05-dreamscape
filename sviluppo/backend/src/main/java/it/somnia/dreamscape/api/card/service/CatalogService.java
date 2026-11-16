package it.somnia.dreamscape.api.card.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.dto.response.FactionAdvantageResponse;
import it.somnia.dreamscape.api.card.dto.response.FactionResponse;
import it.somnia.dreamscape.api.card.dto.response.RarityRuleResponse;
import it.somnia.dreamscape.api.card.dto.response.RulesResponse;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.Faction;
import it.somnia.dreamscape.api.card.model.FactionAdvantage;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.card.repository.CardSpecifications;
import it.somnia.dreamscape.api.card.repository.FactionAdvantageRepository;
import it.somnia.dreamscape.api.card.repository.FactionRepository;
import it.somnia.dreamscape.api.card.repository.RarityRepository;
import it.somnia.dreamscape.api.card.service.CardExceptions.CardNotFoundException;
import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.match.model.GameMode;
import it.somnia.dreamscape.api.match.repository.GameModeRepository;
import lombok.RequiredArgsConstructor;

/**
 * Il catalogo pubblico: le carte, le fazioni e i numeri del regolamento. Si legge anche senza accesso.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    // Le carte "in evidenza" della Home sono le Leggendarie normali, Lord compresi
    private static final String FEATURED_RARITY_CODE = "LEGENDARY";

    private final CardRepository cardRepository;
    private final FactionRepository factionRepository;
    private final RarityRepository rarityRepository;
    private final FactionAdvantageRepository factionAdvantageRepository;
    private final GameModeRepository gameModeRepository;
    private final CardMapper cardMapper;
    private final GameProperties gameProperties;

    /**
     * Restituisce una pagina del catalogo ({@code GET /cards}), in ordine di codice.
     *
     * <p>Ci sono solo le carte normali attive: le full art si leggono con {@link #getCard(UUID)} e le carte
     * disattivate non fanno più parte del gioco. I filtri vuoti non filtrano; un codice di fazione o di
     * rarità che non esiste restituisce una pagina vuota.
     *
     * @param factionCode codice della fazione, es. {@code DREAMS}; può essere {@code null}
     * @param rarityCode  codice della rarità, es. {@code RARE}; può essere {@code null}
     * @param search      parte del nome, senza distinguere maiuscole e minuscole; può essere {@code null}
     * @param featured    {@code true} per le sole carte in evidenza (le Leggendarie)
     * @param page        numero della pagina, da 0
     * @param size        carte per pagina
     * @return la pagina di carte con i totali
     */
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> listCards(String factionCode, String rarityCode, String search,
            boolean featured, int page, int size) {
        List<Specification<Card>> filters = new ArrayList<>();
        filters.add(CardSpecifications.isActiveBaseCard());
        if (hasText(factionCode)) {
            filters.add(CardSpecifications.hasFaction(factionCode.strip()));
        }
        if (hasText(rarityCode)) {
            filters.add(CardSpecifications.hasRarity(rarityCode.strip()));
        }
        if (hasText(search)) {
            filters.add(CardSpecifications.nameContains(search.strip()));
        }
        if (featured) {
            filters.add(CardSpecifications.hasRarity(FEATURED_RARITY_CODE));
        }
        Page<Card> cards = cardRepository.findAll(Specification.allOf(filters),
                PageRequest.of(page, size, Sort.by("code")));
        List<CardResponse> items = cardMapper.toResponses(cards.getContent());
        return new PageResponse<>(items, cards.getNumber(), cards.getSize(), cards.getTotalElements(),
                cards.getTotalPages());
    }

    /**
     * Restituisce una carta del catalogo ({@code GET /cards/{cardId}}), anche full art.
     *
     * <p>Restituisce anche le carte disattivate: chi le possiede le tiene nella collezione e ne vede il
     * dettaglio (decisione 026).
     *
     * @param cardId l'id della carta
     * @return la carta con il suo effetto
     * @throws CardNotFoundException se la carta non esiste
     */
    @Transactional(readOnly = true)
    public CardResponse getCard(UUID cardId) {
        return cardRepository.findById(cardId)
                .map(cardMapper::toResponse)
                .orElseThrow(CardNotFoundException::new);
    }

    /**
     * Restituisce le tre fazioni ({@code GET /factions}) con contro chi sono forti e contro chi sono deboli.
     *
     * @return le fazioni in ordine di codice
     */
    @Transactional(readOnly = true)
    public List<FactionResponse> listFactions() {
        List<FactionAdvantage> advantages = factionAdvantageRepository.findAllByOrderByIdAsc();
        return factionRepository.findAllByOrderByCodeAsc().stream()
                .map(faction -> FactionResponse.from(faction, strongAgainst(faction, advantages),
                        weakAgainst(faction, advantages)))
                .toList();
    }

    /**
     * Restituisce i numeri del regolamento ({@code GET /rules}).
     *
     * <p>Carte per mazzo dalla configurazione, punti vita e limite di round dalla modalità BASE,
     * limiti delle rarità e vantaggi di fazione dal database: se l'admin li cambia, cambiano anche qui.
     *
     * @return i numeri del regolamento
     * @throws IllegalStateException se nel database manca la modalità BASE, creata da V6
     */
    @Transactional(readOnly = true)
    public RulesResponse getRules() {
        GameMode baseMode = gameModeRepository.findByCode(GameMode.BASE_CODE)
                .orElseThrow(() -> new IllegalStateException("Manca la modalità BASE in game_modes"));
        List<RarityRuleResponse> rarities = rarityRepository.findAllByOrderBySortOrderAsc().stream()
                .map(RarityRuleResponse::from)
                .toList();
        List<FactionAdvantageResponse> advantages = factionAdvantageRepository.findAllByOrderByIdAsc().stream()
                .map(FactionAdvantageResponse::from)
                .toList();
        return new RulesResponse(gameProperties.deckSize(), baseMode.getStartingLifePoints(),
                baseMode.getRoundLimit(), rarities, advantages);
    }

    private static List<String> strongAgainst(Faction faction, List<FactionAdvantage> advantages) {
        return advantages.stream()
                .filter(advantage -> advantage.getFaction().getId().equals(faction.getId()))
                .map(advantage -> advantage.getTargetFaction().getCode())
                .toList();
    }

    private static List<String> weakAgainst(Faction faction, List<FactionAdvantage> advantages) {
        return advantages.stream()
                .filter(advantage -> advantage.getTargetFaction().getId().equals(faction.getId()))
                .map(advantage -> advantage.getFaction().getCode())
                .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
