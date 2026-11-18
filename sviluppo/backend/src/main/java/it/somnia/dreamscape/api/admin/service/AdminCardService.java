package it.somnia.dreamscape.api.admin.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.admin.dto.request.AdminCardRequest;
import it.somnia.dreamscape.api.admin.dto.response.AdminCardResponse;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.CardCodeTakenException;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.CardNameTakenException;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.FullArtExistsException;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.InvalidAdminFieldException;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;
import it.somnia.dreamscape.api.card.model.CardSet;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.Effect;
import it.somnia.dreamscape.api.card.model.Faction;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.card.repository.AllCardsSpecifications;
import it.somnia.dreamscape.api.card.repository.CardEffectRepository;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.card.repository.CardSetRepository;
import it.somnia.dreamscape.api.card.repository.EffectRepository;
import it.somnia.dreamscape.api.card.repository.FactionRepository;
import it.somnia.dreamscape.api.card.repository.RarityRepository;
import it.somnia.dreamscape.api.card.service.CardExceptions.CardNotFoundException;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.deck.repository.DeckCardsRepository;
import lombok.RequiredArgsConstructor;

/**
 * Le carte nel backoffice ({@code /admin/cards}): elenco, dettaglio, creazione e modifica.
 *
 * <p>Le carte non si cancellano: si disattivano ({@code isActive: false}). Una carta disattivata non esce dai
 * pacchetti e rende non validi i mazzi che la usano, ma chi la possiede la tiene (decisione 026). La full art è
 * una carta a parte senza dati propri, oggi solo per le Leggendarie (decisione 025). ATT e DIF si possono
 * cambiare anche dopo che la carta è stata pescata (decisione 016).
 */
@Service
@RequiredArgsConstructor
public class AdminCardService {

    // Oggi la full art c'è solo per le Leggendarie (decisione 025)
    private static final String FULL_ART_RARITY = "LEGENDARY";
    private static final String FULL_ART_SUFFIX = "-FA";
    private static final String IMAGES_PREFIX = "/images/";
    private static final BigDecimal DEFAULT_FOCUS_Y = new BigDecimal("0.40");

    private final CardRepository cardRepository;
    private final CardEffectRepository cardEffectRepository;
    private final CardSetRepository cardSetRepository;
    private final FactionRepository factionRepository;
    private final RarityRepository rarityRepository;
    private final EffectRepository effectRepository;
    private final DeckCardsRepository deckCardsRepository;

    /**
     * Restituisce una pagina di carte, comprese full art e carte disattivate, in ordine di codice.
     *
     * @param factionCode filtro sulla fazione; può essere {@code null}
     * @param rarityCode  filtro sulla rarità; può essere {@code null}
     * @param search      parte del nome o del codice; può essere {@code null}
     * @param active      {@code true} solo attive, {@code false} solo disattivate, {@code null} tutte
     * @param fullArt     {@code true} solo full art, {@code false} solo carte normali, {@code null} tutte
     * @param page        numero della pagina, da 0
     * @param size        carte per pagina
     * @return la pagina di carte con i totali
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCardResponse> listCards(String factionCode, String rarityCode, String search,
            Boolean active, Boolean fullArt, int page, int size) {
        List<Specification<Card>> filters = new ArrayList<>();
        filters.add((root, query, cb) -> cb.conjunction());
        if (hasText(factionCode)) {
            filters.add(AllCardsSpecifications.hasFaction(factionCode.strip()));
        }
        if (hasText(rarityCode)) {
            filters.add(AllCardsSpecifications.hasRarity(rarityCode.strip()));
        }
        if (hasText(search)) {
            filters.add(AllCardsSpecifications.nameOrCodeContains(search.strip()));
        }
        if (active != null) {
            filters.add(AllCardsSpecifications.isActive(active));
        }
        if (fullArt != null) {
            filters.add(AllCardsSpecifications.isFullArt(fullArt));
        }
        Page<Card> cards = cardRepository.findAll(Specification.allOf(filters), PageRequest.of(page, size,
                Sort.by("code")));
        return new PageResponse<>(toResponses(cards.getContent()), cards.getNumber(), cards.getSize(),
                cards.getTotalElements(), cards.getTotalPages());
    }

    /**
     * Restituisce una carta, anche disattivata.
     *
     * @param cardId l'id della carta
     * @return la carta con il suo effetto e i mazzi che la usano
     * @throws CardNotFoundException se la carta non esiste
     */
    @Transactional(readOnly = true)
    public AdminCardResponse getCard(UUID cardId) {
        return toResponse(findCard(cardId));
    }

    /**
     * Crea una carta normale oppure la full art di una Leggendaria (se c'è {@code baseCardId}).
     *
     * @param request i dati della carta
     * @return la carta creata
     * @throws CardCodeTakenException      se il codice è già usato
     * @throws CardNameTakenException      se il nome è già usato
     * @throws FullArtExistsException      se la carta base ha già la full art
     * @throws CardNotFoundException       se la carta base non esiste
     * @throws InvalidAdminFieldException  se un dato non rispetta le regole del catalogo
     */
    @Transactional
    public AdminCardResponse createCard(AdminCardRequest request) {
        String code = request.code().strip();
        if (cardRepository.findByCode(code).isPresent()) {
            throw new CardCodeTakenException();
        }
        Card card;
        if (request.baseCardId() != null) {
            Card base = findCard(request.baseCardId());
            checkFullArt(request, code, base);
            card = new Card(code, base.getCardSet());
            card.setBaseCard(base);
            card.setVariantType(CardVariantType.FULL_ART);
        } else {
            card = new Card(code, cardSet(request.cardSetCode()));
            applyCardData(card, request);
        }
        applyIllustration(card, request, true);
        card.setActive(request.isActive() == null || request.isActive());
        saveCard(card);
        if (card.getBaseCard() == null) {
            applyEffect(card, request, null);
        }
        return toResponse(card);
    }

    /**
     * Modifica una carta. Il codice e la carta base non si cambiano.
     *
     * <p>Di una full art si cambiano solo illustrazione, ritaglio e stato attivo: gli altri dati sono della carta
     * base. Le modifiche a ATT, DIF ed effetto valgono anche per la full art e per le partite future.
     *
     * @param cardId  la carta da modificare
     * @param request i dati nuovi
     * @return la carta modificata
     * @throws CardNotFoundException      se la carta non esiste
     * @throws CardNameTakenException     se il nome è già usato da un'altra carta
     * @throws InvalidAdminFieldException se un dato non rispetta le regole del catalogo
     */
    @Transactional
    public AdminCardResponse updateCard(UUID cardId, AdminCardRequest request) {
        Card card = findCard(cardId);
        if (!card.getCode().equals(request.code().strip())) {
            throw new InvalidAdminFieldException("code", "Il codice di una carta non si cambia.");
        }
        UUID baseId = card.getBaseCard() != null ? card.getBaseCard().getId() : null;
        if (baseId == null ? request.baseCardId() != null : !baseId.equals(request.baseCardId())) {
            throw new InvalidAdminFieldException("baseCardId", "La carta base non si cambia.");
        }
        if (card.getBaseCard() != null) {
            requireNoOwnData(request);
        } else {
            card.setCardSet(cardSet(request.cardSetCode()));
            applyCardData(card, request);
            checkLordAndFullArt(card);
        }
        applyIllustration(card, request, false);
        if (request.isActive() != null) {
            card.setActive(request.isActive());
        }
        saveCard(card);
        if (card.getBaseCard() == null) {
            applyEffect(card, request, cardEffectRepository.findByCard(card).orElse(null));
        }
        return toResponse(card);
    }

    // Una full art: codice della base + "-FA", solo per una Leggendaria normale che non ce l'ha ancora
    private void checkFullArt(AdminCardRequest request, String code, Card base) {
        if (base.getBaseCard() != null) {
            throw new InvalidAdminFieldException("baseCardId", "Una versione non può avere a sua volta versioni.");
        }
        if (!FULL_ART_RARITY.equals(base.getRarity().getCode())) {
            throw new InvalidAdminFieldException("baseCardId", "Oggi la full art c'è solo per le Leggendarie.");
        }
        if (cardRepository.findByBaseCardAndVariantType(base, CardVariantType.FULL_ART).isPresent()) {
            throw new FullArtExistsException();
        }
        if (!code.equals(base.getCode() + FULL_ART_SUFFIX)) {
            throw new InvalidAdminFieldException("code", "Il codice della full art è " + base.getCode()
                    + FULL_ART_SUFFIX + ".");
        }
        requireNoOwnData(request);
    }

    // La versione prende nome, testi, statistiche ed effetto dalla carta base (chk_cards_variant)
    private static void requireNoOwnData(AdminCardRequest request) {
        if (request.name() != null || request.description() != null || request.attack() != null
                || request.defense() != null || request.factionCode() != null || request.rarityCode() != null
                || request.effectCode() != null || request.effectValue() != null) {
            throw new InvalidAdminFieldException("baseCardId",
                    "La full art prende nome, testi, statistiche ed effetto dalla carta base: lasciali vuoti.");
        }
    }

    private void applyCardData(Card card, AdminCardRequest request) {
        String name = required(request.name(), "name", "Scrivi il nome della carta.").strip();
        cardRepository.findByName(name)
                .filter(other -> !other.getId().equals(card.getId()))
                .ifPresent(other -> {
                    throw new CardNameTakenException();
                });
        card.setName(name);
        card.setDescription(hasText(request.description()) ? request.description().strip() : null);
        card.setAttack(required(request.attack(), "attack", "Scrivi l'attacco."));
        card.setDefense(required(request.defense(), "defense", "Scrivi la difesa."));
        card.setFaction(faction(request.factionCode()));
        card.setRarity(rarity(request.rarityCode()));
    }

    // Nella modifica, URL e ritaglio che mancano (null) restano quelli di prima: così un PUT con i soli dati
    // della carta non cancella l'illustrazione. Le due URL vuote ("") tolgono l'illustrazione
    private static void applyIllustration(Card card, AdminCardRequest request, boolean creating) {
        if (creating || request.illustrationUrl() != null || request.illustrationThumbUrl() != null) {
            String url = blankToNull(request.illustrationUrl());
            String thumbUrl = blankToNull(request.illustrationThumbUrl());
            if ((url == null) != (thumbUrl == null)) {
                throw new InvalidAdminFieldException("illustrationThumbUrl",
                        "Servono tutte e due le copie dell'illustrazione: grande e miniatura.");
            }
            if (url != null && (!url.startsWith(IMAGES_PREFIX) || !thumbUrl.startsWith(IMAGES_PREFIX))) {
                throw new InvalidAdminFieldException("illustrationUrl",
                        "Usa gli indirizzi restituiti dal caricamento delle immagini (/images/...).");
            }
            card.setIllustrationUrl(url);
            card.setIllustrationThumbUrl(thumbUrl);
        }
        if (request.illustrationFocusY() != null) {
            card.setIllustrationFocusY(request.illustrationFocusY());
        } else if (creating) {
            card.setIllustrationFocusY(DEFAULT_FOCUS_Y);
        }
    }

    // Il Lord resta una Leggendaria della sua fazione (decisione 001) e una carta con la full art resta
    // Leggendaria (decisione 025): altrimenti fazione e full art resterebbero collegate a una carta sbagliata
    private void checkLordAndFullArt(Card card) {
        boolean legendary = FULL_ART_RARITY.equals(card.getRarity().getCode());
        factionRepository.findByLordCard(card).ifPresent(lordOf -> {
            if (!lordOf.getId().equals(card.getFaction().getId())) {
                throw new InvalidAdminFieldException("factionCode",
                        "Questa carta è il Lord di " + lordOf.getName() + ": la fazione non si cambia.");
            }
            if (!legendary) {
                throw new InvalidAdminFieldException("rarityCode",
                        "Questa carta è il Lord di " + lordOf.getName() + ": resta Leggendaria.");
            }
        });
        if (!legendary && cardRepository.findByBaseCardAndVariantType(card, CardVariantType.FULL_ART).isPresent()) {
            throw new InvalidAdminFieldException("rarityCode", "Questa carta ha la full art: resta Leggendaria.");
        }
    }

    // Due richieste nello stesso momento possono superare tutte e due i controlli su codice e nome: il vincolo
    // UNIQUE del database ferma la seconda, che riceve lo stesso errore dei controlli
    private void saveCard(Card card) {
        try {
            cardRepository.saveAndFlush(card);
        } catch (DataIntegrityViolationException e) {
            String cause = String.valueOf(e.getMostSpecificCause().getMessage());
            if (cause.contains("uq_cards_code")) {
                throw new CardCodeTakenException();
            }
            if (cause.contains("uq_cards_name")) {
                throw new CardNameTakenException();
            }
            if (cause.contains("uq_cards_full_art")) {
                throw new FullArtExistsException();
            }
            throw e;
        }
    }

    // Al massimo un effetto per carta; il valore X solo per gli effetti che lo hanno (effects.has_value)
    private void applyEffect(Card card, AdminCardRequest request, CardEffect current) {
        if (!hasText(request.effectCode())) {
            if (request.effectValue() != null) {
                throw new InvalidAdminFieldException("effectValue", "Il valore X serve solo con un effetto.");
            }
            if (current != null) {
                cardEffectRepository.delete(current);
            }
            return;
        }
        Effect effect = effectRepository.findByCode(request.effectCode().strip())
                .orElseThrow(() -> new InvalidAdminFieldException("effectCode", "Questo effetto non esiste."));
        boolean unchanged = current != null && current.getEffect().getId().equals(effect.getId());
        if (!unchanged && !effect.isActive()) {
            throw new InvalidAdminFieldException("effectCode", "Questo effetto è spento: non si assegna più.");
        }
        if (effect.hasValue() && request.effectValue() == null) {
            throw new InvalidAdminFieldException("effectValue", "Questo effetto ha bisogno del valore X.");
        }
        if (!effect.hasValue() && request.effectValue() != null) {
            throw new InvalidAdminFieldException("effectValue", "Questo effetto non ha il valore X.");
        }
        if (current == null) {
            cardEffectRepository.save(new CardEffect(card, effect, request.effectValue()));
        } else {
            current.setEffect(effect);
            current.setEffectValue(request.effectValue());
        }
    }

    private AdminCardResponse toResponse(Card card) {
        return toResponses(List.of(card)).getFirst();
    }

    // Tre query per tutte le carte insieme, non per ogni carta: effetti, versioni delle carte normali e mazzi.
    // deckCount di una carta normale conta anche i mazzi che usano la sua full art
    private List<AdminCardResponse> toResponses(List<Card> cards) {
        List<Card> baseCards = cards.stream().filter(card -> card.getBaseCard() == null).toList();
        Map<UUID, CardEffect> effects = new HashMap<>();
        Map<UUID, Set<UUID>> countedCards = new HashMap<>();
        cards.forEach(card -> countedCards.computeIfAbsent(card.getId(), id -> new HashSet<>()).add(card.getId()));
        if (!baseCards.isEmpty()) {
            cardEffectRepository.findByCardIn(baseCards)
                    .forEach(effect -> effects.put(effect.getCard().getId(), effect));
            cardRepository.findByBaseCardIn(baseCards)
                    .forEach(version -> countedCards.get(version.getBaseCard().getId()).add(version.getId()));
        }
        Set<UUID> allCardIds = new HashSet<>();
        countedCards.values().forEach(allCardIds::addAll);
        Map<UUID, Set<UUID>> decksByCard = new HashMap<>();
        if (!allCardIds.isEmpty()) {
            deckCardsRepository.findUsages(allCardIds).forEach(usage ->
                    decksByCard.computeIfAbsent(usage.getCardId(), id -> new HashSet<>()).add(usage.getDeckId()));
        }
        return cards.stream().map(card -> {
            Set<UUID> decks = new HashSet<>();
            countedCards.get(card.getId()).forEach(id -> decks.addAll(decksByCard.getOrDefault(id, Set.of())));
            return AdminCardResponse.from(card, effects.get(card.getId()), decks.size());
        }).toList();
    }

    private Card findCard(UUID cardId) {
        return cardRepository.findById(cardId).orElseThrow(CardNotFoundException::new);
    }

    private CardSet cardSet(String code) {
        return cardSetRepository.findByCode(required(code, "cardSetCode", "Scegli l'espansione.").strip())
                .orElseThrow(() -> new InvalidAdminFieldException("cardSetCode", "Questa espansione non esiste."));
    }

    private Faction faction(String code) {
        return factionRepository.findByCode(required(code, "factionCode", "Scegli la fazione.").strip())
                .orElseThrow(() -> new InvalidAdminFieldException("factionCode", "Questa fazione non esiste."));
    }

    private Rarity rarity(String code) {
        return rarityRepository.findByCode(required(code, "rarityCode", "Scegli la rarità.").strip())
                .orElseThrow(() -> new InvalidAdminFieldException("rarityCode", "Questa rarità non esiste."));
    }

    private static <T> T required(T value, String field, String message) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            throw new InvalidAdminFieldException(field, message);
        }
        return value;
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.strip() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
