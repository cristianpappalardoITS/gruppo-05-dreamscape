package it.somnia.dreamscape.api.pack.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.model.Faction;
import it.somnia.dreamscape.api.card.repository.FactionRepository;
import it.somnia.dreamscape.api.pack.dto.request.WelcomeRequest;
import it.somnia.dreamscape.api.pack.dto.response.OpenedCardResponse;
import it.somnia.dreamscape.api.pack.dto.response.WelcomeOpeningResponse;
import it.somnia.dreamscape.api.pack.dto.response.WelcomeResponse;
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackOpening;
import it.somnia.dreamscape.api.pack.model.PackOpeningSource;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import it.somnia.dreamscape.api.pack.service.PackExceptions.InvalidStarterFactionException;
import it.somnia.dreamscape.api.pack.service.PackExceptions.WelcomeAlreadyClaimedException;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * La pagina Benvenuto: il giocatore sceglie una fazione e riceve, una volta sola, il set iniziale di quella
 * fazione e i pacchetti di benvenuto con carte di tutte le fazioni (decisione 014).
 */
@Service
@RequiredArgsConstructor
public class WelcomeService {

    private final UserService userService;
    private final FactionRepository factionRepository;
    private final PackRepository packRepository;
    private final PackOpeningRepository packOpeningRepository;
    private final PackOpener packOpener;
    private final OpenedCardMapper openedCardMapper;

    /**
     * Restituisce lo stato del Benvenuto ({@code GET /me/welcome}).
     *
     * @param userId l'utente collegato
     * @return la fazione scelta con il set iniziale e i pacchetti di benvenuto;
     *         fazione {@code null} e liste vuote se non l'ha ancora scelta
     */
    @Transactional(readOnly = true)
    public WelcomeResponse getWelcome(UUID userId) {
        User user = userService.getActiveUser(userId);
        if (user.getStarterFaction() == null) {
            return WelcomeResponse.notClaimed();
        }
        List<PackOpening> gifts = packOpeningRepository.findByUserIdAndSourceOrderByIdAsc(userId,
                PackOpeningSource.SIGNUP);
        return toResponse(user.getStarterFaction(), gifts);
    }

    /**
     * Registra la fazione scelta e dà i regali del Benvenuto ({@code POST /me/welcome}).
     *
     * <p>Tutto in una transazione: il set iniziale della fazione (mai oltre le copie massime) e i pacchetti
     * di benvenuto attivi, tutti aperti subito e messi nella collezione. Il mazzo non si crea in automatico.
     * La riga dell'utente è bloccata, quindi due richieste insieme non danno i regali due volte.
     *
     * @param userId  l'utente collegato
     * @param request la fazione scelta
     * @return lo stato del Benvenuto, come {@link #getWelcome(UUID)}
     * @throws WelcomeAlreadyClaimedException se la fazione è già stata scelta
     * @throws InvalidStarterFactionException se la fazione non esiste
     * @throws IllegalStateException          se manca il set iniziale attivo di quella fazione
     */
    @Transactional
    public WelcomeResponse claimWelcome(UUID userId, WelcomeRequest request) {
        User user = userService.getActiveUserForUpdate(userId);
        if (user.getStarterFaction() != null) {
            throw new WelcomeAlreadyClaimedException();
        }
        Faction faction = factionRepository.findByCode(request.starterFaction())
                .orElseThrow(InvalidStarterFactionException::new);
        Pack starterPack = packRepository
                .findFirstByPackTypeAndFactionAndActiveTrueOrderByNameAsc(PackType.STARTER, faction)
                .orElseThrow(() -> new IllegalStateException("Manca il set iniziale attivo di " + faction.getCode()));

        user.setStarterFaction(faction);
        PackOpening starter = packOpener.open(user, starterPack, null, PackOpeningSource.SIGNUP, 0);
        List<PackOpening> welcomeOpenings = packRepository.findByPackTypeAndActiveTrueOrderByNameAsc(PackType.WELCOME)
                .stream()
                .map(pack -> packOpener.open(user, pack, null, PackOpeningSource.SIGNUP, 0))
                .toList();
        List<PackOpening> gifts = new ArrayList<>(welcomeOpenings);
        gifts.addFirst(starter);
        return toResponse(faction, gifts);
    }

    // Il set iniziale è l'apertura del pacchetto STARTER; le altre aperture regalate sono i pacchetti di benvenuto
    private WelcomeResponse toResponse(Faction faction, List<PackOpening> gifts) {
        List<OpenedCardResponse> starterCards = gifts.stream()
                .filter(opening -> opening.getPack().getPackType() == PackType.STARTER)
                .flatMap(opening -> openedCardMapper.toResponses(PackOpener.cardsOf(opening)).stream())
                .toList();
        List<WelcomeOpeningResponse> welcomeOpenings = gifts.stream()
                .filter(opening -> opening.getPack().getPackType() == PackType.WELCOME)
                .map(opening -> new WelcomeOpeningResponse(opening.getId(), opening.getPack().getName(),
                        openedCardMapper.toResponses(PackOpener.cardsOf(opening))))
                .toList();
        return new WelcomeResponse(faction.getCode(), starterCards, welcomeOpenings);
    }
}
