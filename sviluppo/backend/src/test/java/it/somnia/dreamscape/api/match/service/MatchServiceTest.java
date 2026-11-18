package it.somnia.dreamscape.api.match.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.model.DeckCards;
import it.somnia.dreamscape.api.deck.service.DeckCheck;
import it.somnia.dreamscape.api.match.dto.response.MatchResponse;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.engine.battle.BattleEngine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// MatchService con le carte di prova (V900): la partita la gioca il motore dentro il backend. I mazzi si giocano
// così come sono (li controlla chi chiama), quindi qui non serve che siano validi
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MatchServiceTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void play_twoDecks_savesResultRulesSeedAndLog() {
        // Given
        User anna = user("anna_test");
        User bruno = user("bruno_test");

        // When
        Match match = matchService.play(MatchType.CASUAL, anna, deck(anna, "TEST-001", "TEST-002"), bruno,
                deck(bruno, "TEST-014", "TEST-015"), null);

        // Then
        assertThat(match.getId()).isNotNull();
        assertThat(match.getPlayedAt()).isNotNull();
        assertThat(match.getStartingLifePoints()).isEqualTo(20);
        assertThat(match.getRoundLimit()).isEqualTo(60);
        assertThat(match.getTotalRounds()).isBetween(1, 60);
        assertThat(match.getChallengerRatingChange()).isNull();
        assertThat(match.getWinner() == null || match.getWinner().equals(anna) || match.getWinner().equals(bruno))
                .isTrue();
        JsonNode log = objectMapper.readTree(match.getRoundLog());
        assertThat(log.path("engineVersion").asString()).isEqualTo(BattleEngine.ENGINE_VERSION);
        assertThat(log.path("rounds").size()).isEqualTo(match.getTotalRounds());
    }

    @Test
    void play_fullArtInTheDeck_snapshotHasTheFullArtIdAndTheBaseCardData() {
        // Given: la full art di TEST-013 (Leggendaria con l'effetto REST 3)
        User anna = user("anna_test");
        User bruno = user("bruno_test");
        Card base = card("TEST-013");

        // When
        Match match = matchService.play(MatchType.CASUAL, anna, deck(anna, "TEST-013-FA", "TEST-001"), bruno,
                deck(bruno, "TEST-014"), null);

        // Then
        JsonNode fullArt = findCard(objectMapper.readTree(match.getChallengerDeckSnapshot()), "TEST-013-FA");
        assertThat(fullArt.path("cardId").asString()).isEqualTo(card("TEST-013-FA").getId().toString());
        assertThat(fullArt.path("baseCardId").asString()).isEqualTo(base.getId().toString());
        assertThat(fullArt.path("variantType").asString()).isEqualTo("FULL_ART");
        assertThat(fullArt.path("name").asString()).isEqualTo(base.getName());
        assertThat(fullArt.path("attack").asInt()).isEqualTo(base.getAttack());
        assertThat(fullArt.path("faction").asString()).isEqualTo("DREAMS");
        assertThat(fullArt.path("rarity").asString()).isEqualTo("LEGENDARY");
        assertThat(fullArt.path("quantity").asInt()).isEqualTo(2);
        assertThat(fullArt.path("effects").get(0).path("code").asString()).isEqualTo("REST");
        assertThat(fullArt.path("effects").get(0).path("value").asInt()).isEqualTo(3);
        assertThat(fullArt.path("effects").get(0).path("triggerEvent").asString()).isNotBlank();
    }

    @Test
    void getMatch_player_readsTheResultWithReplayAndDecks() {
        // Given
        User anna = user("anna_test");
        User bruno = user("bruno_test");
        Match match = matchService.play(MatchType.CASUAL, anna, deck(anna, "TEST-001"), bruno,
                deck(bruno, "TEST-014"), null);

        // When
        MatchResponse response = matchService.getMatch(bruno.getId(), match.getId());

        // Then
        assertThat(response.challenger().username()).isEqualTo("anna_test");
        assertThat(response.ratingChange()).isNull();
        assertThat(response.finalLifePoints().challenger()).isEqualTo(match.getChallengerLifePoints());
        assertThat(response.rounds().size()).isEqualTo(match.getTotalRounds());
        assertThat(response.coinTossWinner()).isIn("CHALLENGER", "OPPONENT");
        assertThat(findCard(response.decks().opponent(), "TEST-014").path("quantity").asInt()).isEqualTo(2);
    }

    // Un mazzo di prova non salvato: due copie di ogni carta
    private DeckCheck deck(User owner, String... codes) {
        Deck deck = new Deck(owner, "Mazzo di prova");
        List<DeckCards> cards = new ArrayList<>();
        for (String code : codes) {
            cards.add(new DeckCards(deck, card(code), 2));
        }
        return new DeckCheck(deck, cards, List.of());
    }

    private static JsonNode findCard(JsonNode snapshot, String code) {
        for (JsonNode card : snapshot.path("cards")) {
            if (code.equals(card.path("cardCode").asString())) {
                return card;
            }
        }
        throw new AssertionError("Carta " + code + " non trovata nella copia del mazzo");
    }

    private Card card(String code) {
        return cardRepository.findByCode(code).orElseThrow();
    }

    private User user(String username) {
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow();
    }
}
