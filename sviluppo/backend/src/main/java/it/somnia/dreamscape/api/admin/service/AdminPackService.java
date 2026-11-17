package it.somnia.dreamscape.api.admin.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.admin.dto.request.AdminPackRequest;
import it.somnia.dreamscape.api.admin.dto.response.AdminPackResponse;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.InvalidAdminFieldException;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.PackNameTakenException;
import it.somnia.dreamscape.api.card.model.CardSet;
import it.somnia.dreamscape.api.card.model.Faction;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.card.repository.CardSetRepository;
import it.somnia.dreamscape.api.card.repository.FactionRepository;
import it.somnia.dreamscape.api.card.repository.RarityRepository;
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackCover;
import it.somnia.dreamscape.api.pack.model.PackSlot;
import it.somnia.dreamscape.api.pack.model.PackSlotRarity;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.PackCoverRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import it.somnia.dreamscape.api.pack.repository.PackSlotRarityRepository;
import it.somnia.dreamscape.api.pack.repository.PackSlotRepository;
import it.somnia.dreamscape.api.pack.service.PackExceptions.PackNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * I pacchetti nel backoffice ({@code /admin/packs}): elenco, dettaglio, creazione e modifica (decisioni 012 e 030).
 *
 * <p>I pacchetti non si cancellano: si spengono ({@code isActive: false}), così le aperture già fatte restano
 * valide. Gli slot si sostituiscono a ogni modifica; le copertine si modificano, si aggiungono o si spengono.
 */
@Service
@RequiredArgsConstructor
public class AdminPackService {

    // I pesi di uno slot sono percentuali: la loro somma è sempre 100
    static final int SLOT_WEIGHT_TOTAL = 100;

    private final PackRepository packRepository;
    private final PackSlotRepository packSlotRepository;
    private final PackSlotRarityRepository packSlotRarityRepository;
    private final PackCoverRepository packCoverRepository;
    private final CardSetRepository cardSetRepository;
    private final FactionRepository factionRepository;
    private final RarityRepository rarityRepository;
    private final EntityManager entityManager;

    /**
     * Restituisce tutti i pacchetti, anche spenti: prima quelli del negozio, poi set iniziali e benvenuto,
     * ognuno dal più economico e per nome.
     *
     * @return i pacchetti con slot e copertine
     */
    @Transactional(readOnly = true)
    public List<AdminPackResponse> listPacks() {
        return packRepository.findAll().stream()
                .sorted(Comparator.comparing(Pack::getPackType).thenComparingInt(Pack::getPriceCredits)
                        .thenComparing(Pack::getName))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Restituisce un pacchetto, anche spento.
     *
     * @param packId l'id del pacchetto
     * @return il pacchetto con slot e copertine
     * @throws PackNotFoundException se il pacchetto non esiste
     */
    @Transactional(readOnly = true)
    public AdminPackResponse getPack(UUID packId) {
        return toResponse(findPack(packId));
    }

    /**
     * Crea un pacchetto con i suoi slot e le sue copertine.
     *
     * @param request i dati del pacchetto
     * @return il pacchetto creato
     * @throws PackNameTakenException     se il nome è già usato
     * @throws InvalidAdminFieldException se un dato non rispetta le regole (prezzo, periodo, pesi, codici)
     */
    @Transactional
    public AdminPackResponse createPack(AdminPackRequest request) {
        String name = request.name().strip();
        if (packRepository.findByName(name).isPresent()) {
            throw new PackNameTakenException();
        }
        validate(request);
        Pack pack = new Pack(name, request.packType());
        applyData(pack, request);
        packRepository.saveAndFlush(pack);
        saveSlots(pack, request.slots());
        saveCovers(pack, request.covers());
        return reload(pack.getId());
    }

    /**
     * Modifica un pacchetto. Gli slot sostituiscono quelli di prima.
     *
     * @param packId  il pacchetto da modificare
     * @param request i dati nuovi
     * @return il pacchetto modificato
     * @throws PackNotFoundException      se il pacchetto non esiste
     * @throws PackNameTakenException     se il nome è già usato da un altro pacchetto
     * @throws InvalidAdminFieldException se un dato non rispetta le regole
     */
    @Transactional
    public AdminPackResponse updatePack(UUID packId, AdminPackRequest request) {
        Pack pack = findPack(packId);
        String name = request.name().strip();
        packRepository.findByName(name)
                .filter(other -> !other.getId().equals(packId))
                .ifPresent(other -> {
                    throw new PackNameTakenException();
                });
        validate(request);
        pack.setName(name);
        pack.setPackType(request.packType());
        applyData(pack, request);
        packSlotRepository.deleteAllSlotsOf(pack);
        Pack reloaded = findPack(packId);
        saveSlots(reloaded, request.slots());
        saveCovers(reloaded, request.covers());
        return reload(packId);
    }

    // Le regole che il database controlla con i CHECK, scritte prima perché l'admin veda un messaggio chiaro
    private void validate(AdminPackRequest request) {
        boolean shop = request.packType() == PackType.SHOP;
        if (shop && request.priceCredits() <= 0) {
            throw new InvalidAdminFieldException("priceCredits", "Un pacchetto del negozio ha un prezzo.");
        }
        if (!shop && request.priceCredits() != 0) {
            throw new InvalidAdminFieldException("priceCredits",
                    "Set iniziali e pacchetti di benvenuto sono gratis: il prezzo è 0.");
        }
        if (request.availableFrom() != null && request.availableUntil() != null
                && !request.availableFrom().isBefore(request.availableUntil())) {
            throw new InvalidAdminFieldException("availableUntil", "La fine della vendita viene dopo l'inizio.");
        }
        for (int i = 0; i < request.slots().size(); i++) {
            AdminPackRequest.Slot slot = request.slots().get(i);
            Set<String> codes = new HashSet<>();
            int total = 0;
            for (AdminPackRequest.SlotRarity rarity : slot.rarities()) {
                if (!codes.add(rarity.rarityCode().strip())) {
                    throw new InvalidAdminFieldException("slots[" + i + "].rarities",
                            "Una rarità compare due volte nello stesso slot.");
                }
                total += rarity.weight();
            }
            if (total != SLOT_WEIGHT_TOTAL) {
                throw new InvalidAdminFieldException("slots[" + i + "].rarities",
                        "Le probabilità dello slot " + (i + 1) + " sommano a " + total + ": devono fare 100.");
            }
        }
        if (request.covers() != null) {
            Set<String> names = new HashSet<>();
            for (AdminPackRequest.Cover cover : request.covers()) {
                if (!names.add(cover.name().strip())) {
                    throw new InvalidAdminFieldException("covers", "Due copertine hanno lo stesso nome.");
                }
            }
        }
    }

    private void applyData(Pack pack, AdminPackRequest request) {
        pack.setDescription(hasText(request.description()) ? request.description().strip() : null);
        pack.setPriceCredits(request.priceCredits());
        pack.setCardSet(hasText(request.cardSetCode()) ? cardSet(request.cardSetCode()) : null);
        pack.setFaction(hasText(request.factionCode()) ? faction(request.factionCode(), "factionCode") : null);
        pack.setFullArtChance(request.fullArtChance());
        pack.setAvailableFrom(request.availableFrom());
        pack.setAvailableUntil(request.availableUntil());
        pack.setImageUrl(hasText(request.imageUrl()) ? request.imageUrl().strip() : null);
        if (request.isActive() != null) {
            pack.setActive(request.isActive());
        }
    }

    private void saveSlots(Pack pack, List<AdminPackRequest.Slot> slots) {
        Map<String, Rarity> rarities = rarityRepository.findAll().stream()
                .collect(Collectors.toMap(Rarity::getCode, Function.identity()));
        for (int i = 0; i < slots.size(); i++) {
            AdminPackRequest.Slot request = slots.get(i);
            PackSlot slot = packSlotRepository.save(new PackSlot(pack, i + 1, request.cardCount()));
            for (AdminPackRequest.SlotRarity rarity : request.rarities()) {
                Rarity found = rarities.get(rarity.rarityCode().strip());
                if (found == null) {
                    throw new InvalidAdminFieldException("slots[" + i + "].rarities", "Questa rarità non esiste.");
                }
                packSlotRarityRepository.save(new PackSlotRarity(slot, found, rarity.weight()));
            }
        }
    }

    // Copertine con id: si modificano; senza id: si aggiungono; quelle che mancano nella richiesta si spengono
    private void saveCovers(Pack pack, List<AdminPackRequest.Cover> requests) {
        List<AdminPackRequest.Cover> covers = requests != null ? requests : List.of();
        Map<UUID, PackCover> existing = packCoverRepository.findByPackOrderBySortOrderAsc(pack).stream()
                .collect(Collectors.toMap(PackCover::getId, Function.identity()));
        Set<UUID> kept = new HashSet<>();
        for (int i = 0; i < covers.size(); i++) {
            AdminPackRequest.Cover request = covers.get(i);
            PackCover cover;
            if (request.id() != null) {
                cover = existing.get(request.id());
                if (cover == null) {
                    throw new InvalidAdminFieldException("covers", "Questa copertina non è di questo pacchetto.");
                }
                kept.add(cover.getId());
                cover.setName(request.name().strip());
                cover.setImageUrl(request.imageUrl().strip());
                cover.setSortOrder(i + 1);
            } else {
                cover = new PackCover(pack, request.name().strip(), request.imageUrl().strip(), i + 1);
            }
            cover.setFaction(hasText(request.factionCode()) ? faction(request.factionCode(), "covers") : null);
            cover.setActive(request.isActive() == null || request.isActive());
            packCoverRepository.save(cover);
        }
        existing.values().stream()
                .filter(cover -> !kept.contains(cover.getId()))
                .forEach(cover -> cover.setActive(false));
    }

    // Rilegge il pacchetto dal database, così slot e copertine sono quelli appena salvati
    private AdminPackResponse reload(UUID packId) {
        entityManager.flush();
        entityManager.clear();
        return toResponse(findPack(packId));
    }

    private AdminPackResponse toResponse(Pack pack) {
        return AdminPackResponse.from(pack, packCoverRepository.findByPackOrderBySortOrderAsc(pack));
    }

    private Pack findPack(UUID packId) {
        return packRepository.findById(packId).orElseThrow(PackNotFoundException::new);
    }

    private CardSet cardSet(String code) {
        return cardSetRepository.findByCode(code.strip())
                .orElseThrow(() -> new InvalidAdminFieldException("cardSetCode", "Questa espansione non esiste."));
    }

    private Faction faction(String code, String field) {
        return factionRepository.findByCode(code.strip())
                .orElseThrow(() -> new InvalidAdminFieldException(field, "Questa fazione non esiste."));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
