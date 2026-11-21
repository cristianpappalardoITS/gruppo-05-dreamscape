package it.somnia.dreamscape.api.match.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;
import it.somnia.dreamscape.api.card.repository.CardEffectRepository;
import it.somnia.dreamscape.api.card.repository.FactionAdvantageRepository;
import it.somnia.dreamscape.api.challenge.model.Challenge;
import it.somnia.dreamscape.api.deck.model.DeckCards;
import it.somnia.dreamscape.api.deck.service.DeckCheck;
import it.somnia.dreamscape.api.league.model.UserLeagueReward;
import it.somnia.dreamscape.api.league.repository.UserLeagueRewardRepository;
import it.somnia.dreamscape.api.match.dto.response.MatchResponse;
import it.somnia.dreamscape.api.match.model.GameMode;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.repository.GameModeRepository;
import it.somnia.dreamscape.api.match.repository.MatchRepository;
import it.somnia.dreamscape.api.match.service.MatchExceptions.EngineUnavailableException;
import it.somnia.dreamscape.api.match.service.MatchExceptions.MatchNotFoundException;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.SimulationRequest;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.FactionAdvantage;
import it.somnia.dreamscape.engine.input.GameRules;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Gioca e rilegge le partite (decisioni 010 e 029). Il backend non conosce le regole del gioco: prepara le copie
 * dei due mazzi, le regole della modalità BASE e il seed, chiede la partita al motore e salva il risultato.
 *
 * <p>Si usa per le sfide tra amici (#94) e servirà al matchmaking (#127), che aggiunge i crediti e applica il
 * rank con {@link it.somnia.dreamscape.api.league.service.RankService}.
 */
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final GameModeRepository gameModeRepository;
    private final FactionAdvantageRepository factionAdvantageRepository;
    private final CardEffectRepository cardEffectRepository;
    private final UserLeagueRewardRepository userLeagueRewardRepository;
    private final BattleSimulator battleSimulator;
    private final RandomGenerator randomGenerator;
    private final ObjectMapper objectMapper;

    /**
     * Gioca una partita e la salva. I mazzi vanno controllati prima da chi chiama: qui si giocano così come sono.
     *
     * @param matchType      CASUAL o RANKED
     * @param challenger     chi avvia la partita (chi ha lanciato la sfida)
     * @param challengerDeck il suo mazzo, già controllato
     * @param opponent       l'avversario
     * @param opponentDeck   il suo mazzo, già controllato
     * @param challenge      la sfida da cui nasce la partita; null per il matchmaking
     * @return la partita salvata
     * @throws EngineUnavailableException se il motore non risponde: non si salva niente
     */
    @Transactional
    public Match play(MatchType matchType, User challenger, DeckCheck challengerDeck, User opponent,
            DeckCheck opponentDeck, Challenge challenge) {
        GameMode mode = gameModeRepository.findByCode(GameMode.BASE_CODE)
                .orElseThrow(() -> new IllegalStateException("Manca la modalità " + GameMode.BASE_CODE));
        Map<UUID, CardEffect> effects = effectsOf(challengerDeck, opponentDeck);
        GameRules rules = new GameRules(mode.getStartingLifePoints(), mode.getRoundLimit(), factionAdvantages());
        long seed = randomGenerator.nextLong();
        SimulationResult result = battleSimulator.simulate(new SimulationRequest(engineDeck(challengerDeck, effects),
                engineDeck(opponentDeck, effects), rules, seed, true));
        if (result.log() == null) {
            throw new IllegalStateException("Il motore ha risposto senza il log, chiesto con withLog = true");
        }
        Match match = Match.builder()
                .matchType(matchType)
                .challenge(challenge)
                .challenger(challenger)
                .opponent(opponent)
                .winner(result.winner() == null ? null : result.winner() == Side.CHALLENGER ? challenger : opponent)
                .endReason(result.endReason())
                .totalRounds(result.totalRounds())
                .startingLifePoints(mode.getStartingLifePoints())
                .roundLimit(mode.getRoundLimit())
                .challengerLifePoints(result.challengerLifePoints())
                .opponentLifePoints(result.opponentLifePoints())
                .randomSeed(seed)
                .challengerDeckSnapshot(objectMapper.writeValueAsString(storedDeck(challengerDeck, effects)))
                .opponentDeckSnapshot(objectMapper.writeValueAsString(storedDeck(opponentDeck, effects)))
                .roundLog(objectMapper.writeValueAsString(result.log()))
                .build();
        // saveAndFlush: la data della partita la scrive Hibernate all'inserimento
        return matchRepository.saveAndFlush(match);
    }

    /**
     * Il risultato di una partita con il replay ({@code GET /matches/{matchId}}), solo per i due giocatori.
     *
     * @param userId  l'utente collegato
     * @param matchId la partita
     * @return il risultato, i round e le copie dei mazzi
     * @throws MatchNotFoundException se la partita non esiste o l'utente non l'ha giocata
     */
    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID userId, UUID matchId) {
        Match match = matchRepository.findWithPlayersById(matchId)
                .filter(found -> found.involves(userId))
                .orElseThrow(MatchNotFoundException::new);
        JsonNode log = objectMapper.readTree(match.getRoundLog());
        MatchResponse.SidesResponse ratingChange = match.getChallengerRatingChange() == null ? null
                : new MatchResponse.SidesResponse(match.getChallengerRatingChange(), match.getOpponentRatingChange());
        return new MatchResponse(
                match.getId(),
                match.getMatchType(),
                UserSummaryResponse.from(match.getChallenger()),
                UserSummaryResponse.from(match.getOpponent()),
                match.getWinner() != null ? match.getWinner().getId() : null,
                ratingChange,
                // I DC per la vittoria nelle classificate arrivano con il matchmaking (#127)
                0,
                leagueReached(userId, matchId),
                match.getEndReason(),
                match.getTotalRounds(),
                match.getStartingLifePoints(),
                new MatchResponse.SidesResponse(match.getChallengerLifePoints(), match.getOpponentLifePoints()),
                log.path("coinTossWinner").asString(),
                match.getPlayedAt(),
                log.path("engineVersion").asString(),
                log.path("rounds"),
                new MatchResponse.DecksResponse(objectMapper.readTree(match.getChallengerDeckSnapshot()),
                        objectMapper.readTree(match.getOpponentDeckSnapshot())));
    }

    // La lega più alta in cui chi guarda è entrato con questa partita, con tutti i premi di lega presi con essa
    // (più di una lega solo se il backoffice ha abbassato le soglie); null se nessuna
    private MatchResponse.LeagueReachedResponse leagueReached(UUID userId, UUID matchId) {
        List<UserLeagueReward> rewards = userLeagueRewardRepository
                .findByUserIdAndMatchIdOrderByLeagueMinRatingDesc(userId, matchId);
        if (rewards.isEmpty()) {
            return null;
        }
        int rewardCredits = rewards.stream().mapToInt(UserLeagueReward::getRewardCredits).sum();
        return new MatchResponse.LeagueReachedResponse(rewards.getFirst().getLeague().getName(), rewardCredits);
    }

    // Gli effetti stanno sulle carte normali: per una full art si usa quello della sua carta base
    private Map<UUID, CardEffect> effectsOf(DeckCheck first, DeckCheck second) {
        List<Card> baseCards = Stream.concat(first.cards().stream(), second.cards().stream())
                .map(deckCard -> baseOf(deckCard.getCard()))
                .distinct()
                .toList();
        Map<UUID, CardEffect> effects = new HashMap<>();
        cardEffectRepository.findByCardIn(baseCards).forEach(effect -> effects.put(effect.getCard().getId(), effect));
        return effects;
    }

    private List<FactionAdvantage> factionAdvantages() {
        return factionAdvantageRepository.findAllByOrderByIdAsc().stream()
                .map(advantage -> new FactionAdvantage(Faction.valueOf(advantage.getFaction().getCode()),
                        Faction.valueOf(advantage.getTargetFaction().getCode()), advantage.getAttackBonus(),
                        advantage.getDefenseBonus()))
                .toList();
    }

    // Il mazzo per il motore: per la full art l'id è il suo, i dati sono quelli della carta base
    private static DeckSnapshot engineDeck(DeckCheck deck, Map<UUID, CardEffect> effects) {
        return new DeckSnapshot(deck.cards().stream().map(deckCard -> {
            Card card = deckCard.getCard();
            Card base = baseOf(card);
            CardEffect effect = effects.get(base.getId());
            List<it.somnia.dreamscape.engine.input.CardEffect> cardEffects = effect == null ? List.of()
                    : List.of(new it.somnia.dreamscape.engine.input.CardEffect(effect.getEffect().getCode(),
                            effect.getEffectValue()));
            return new CardSnapshot(card.getId(), base.getName(), base.getAttack(), base.getDefense(),
                    Faction.valueOf(base.getFaction().getCode()), deckCard.getQuantity(), cardEffects);
        }).toList());
    }

    private static StoredDeckSnapshot storedDeck(DeckCheck deck, Map<UUID, CardEffect> effects) {
        List<StoredDeckSnapshot.Card> cards = deck.cards().stream().map(deckCard -> storedCard(deckCard, effects))
                .toList();
        return new StoredDeckSnapshot(deck.deck().getId(), deck.deck().getName(), cards);
    }

    private static StoredDeckSnapshot.Card storedCard(DeckCards deckCard, Map<UUID, CardEffect> effects) {
        Card card = deckCard.getCard();
        Card base = baseOf(card);
        CardEffect effect = effects.get(base.getId());
        List<StoredDeckSnapshot.Effect> cardEffects = effect == null ? List.of()
                : List.of(new StoredDeckSnapshot.Effect(effect.getEffect().getCode(), effect.getEffect().getName(),
                        effect.getEffect().getTriggerEvent(), effect.getEffectValue()));
        return new StoredDeckSnapshot.Card(card.getId(), card.getCode(),
                card.getBaseCard() != null ? base.getId() : null, card.getVariantType(), base.getName(),
                base.getAttack(), base.getDefense(), base.getFaction().getCode(), base.getRarity().getCode(),
                deckCard.getQuantity(), cardEffects);
    }

    private static Card baseOf(Card card) {
        return card.getBaseCard() != null ? card.getBaseCard() : card;
    }
}
