package it.somnia.dreamscape.api.league.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.model.DeckCards;
import it.somnia.dreamscape.api.deck.service.DeckCheck;
import it.somnia.dreamscape.api.league.model.League;
import it.somnia.dreamscape.api.league.repository.LeagueRepository;
import it.somnia.dreamscape.api.league.service.EloCalculator.RatingChanges;
import it.somnia.dreamscape.api.match.dto.response.MatchResponse;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.service.MatchService;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.UserRepository;

// Rank delle classificate (#106): le partite le gioca il motore dentro il backend con le carte di prova (V900),
// quindi il vincitore non si sceglie e le variazioni attese si calcolano dal vincitore vero
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RankServiceTest {

    @Autowired
    private RankService rankService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private LeagueRepository leagueRepository;

    private User anna;
    private User bruno;

    @BeforeEach
    void setUp() {
        anna = userRepository.findByUsernameIgnoreCase("anna_test").orElseThrow();
        bruno = userRepository.findByUsernameIgnoreCase("bruno_test").orElseThrow();
    }

    @Test
    void applyRankedMatch_firstMatchOfTheDay_updatesBothRatingsWithOppositeChanges() {
        // Given
        Match match = playRanked(anna, bruno);
        RatingChanges expected = EloCalculator.calculate(1000, 1000, challengerScore(match));

        // When
        RankResult result = rankService.applyRankedMatch(match);

        // Then
        assertThat(result.isRated()).isTrue();
        assertThat(result.challengerRatingChange()).isEqualTo(expected.challenger());
        assertThat(result.opponentRatingChange()).isEqualTo(-expected.challenger());
        assertThat(anna.getRating()).isEqualTo(1000 + expected.challenger());
        assertThat(bruno.getRating()).isEqualTo(1000 + expected.opponent());
        assertThat(match.getChallengerRatingChange()).isEqualTo(expected.challenger());
        assertThat(match.getOpponentRatingChange()).isEqualTo(expected.opponent());
    }

    @Test
    void applyRankedMatch_fourthMatchOfTheDayAgainstTheSameOpponent_doesNotChangeTheRank() {
        // Given: tre classificate già contate oggi tra i due, una con i ruoli scambiati
        rankService.applyRankedMatch(playRanked(anna, bruno));
        rankService.applyRankedMatch(playRanked(bruno, anna));
        rankService.applyRankedMatch(playRanked(anna, bruno));
        int annaRating = anna.getRating();
        int brunoRating = bruno.getRating();
        Match fourth = playRanked(bruno, anna);

        // When
        RankResult result = rankService.applyRankedMatch(fourth);

        // Then
        assertThat(result.isRated()).isFalse();
        assertThat(result.challengerLeagues()).isEmpty();
        assertThat(fourth.getChallengerRatingChange()).isNull();
        assertThat(fourth.getOpponentRatingChange()).isNull();
        assertThat(anna.getRating()).isEqualTo(annaRating);
        assertThat(bruno.getRating()).isEqualTo(brunoRating);
    }

    @Test
    void applyRankedMatch_anotherOpponent_hasItsOwnDailyLimit() {
        // Given
        User carla = userRepository.findByUsernameIgnoreCase("carla_test").orElseThrow();
        for (int i = 0; i < RankService.RATED_MATCHES_PER_DAY_PER_OPPONENT; i++) {
            rankService.applyRankedMatch(playRanked(anna, bruno));
        }

        // When
        RankResult result = rankService.applyRankedMatch(playRanked(anna, carla));

        // Then
        assertThat(result.isRated()).isTrue();
    }

    @Test
    void applyRankedMatch_ratingReachesALeague_givesTheRewardAndShowsItInTheResult() {
        // Given: Sognatore con un premio di 150 DC e i due appena sotto la soglia; chi vince ci entra
        League dreamer = leagueRepository.findAll().stream()
                .filter(league -> league.getCode().equals("DREAMER")).findFirst().orElseThrow();
        dreamer.setRewardCredits(150);
        anna.setRating(1099);
        bruno.setRating(1099);
        int annaCredits = anna.getCredits();
        Match match = playRankedUntilSomeoneWins(anna, bruno);
        User winner = match.getWinner().getId().equals(anna.getId()) ? anna : bruno;
        User loser = winner == anna ? bruno : anna;
        int winnerCredits = winner == anna ? annaCredits : bruno.getCredits();

        // When
        RankResult result = rankService.applyRankedMatch(match);

        // Then
        List<?> winnerLeagues = winner == anna ? result.challengerLeagues() : result.opponentLeagues();
        assertThat(winnerLeagues).hasSize(1);
        assertThat(winner.getCredits()).isEqualTo(winnerCredits + 150);
        MatchResponse winnerView = matchService.getMatch(winner.getId(), match.getId());
        assertThat(winnerView.leagueReached()).isEqualTo(new MatchResponse.LeagueReachedResponse("Sognatore", 150));
        assertThat(matchService.getMatch(loser.getId(), match.getId()).leagueReached()).isNull();
    }

    @Test
    void applyRankedMatch_casualMatch_throwsException() {
        Match casual = matchService.play(MatchType.CASUAL, anna, deck(anna), bruno, deck(bruno), null);

        assertThatThrownBy(() -> rankService.applyRankedMatch(casual)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyRankedMatch_sameMatchTwice_throwsException() {
        Match match = playRanked(anna, bruno);
        rankService.applyRankedMatch(match);

        assertThatThrownBy(() -> rankService.applyRankedMatch(match)).isInstanceOf(IllegalArgumentException.class);
    }

    private Match playRanked(User challenger, User opponent) {
        return matchService.play(MatchType.RANKED, challenger, deck(challenger), opponent, deck(opponent), null);
    }

    // Il motore decide il vincitore: si rigioca finché qualcuno non vince. I pareggi rigiocati non passano dal rank,
    // quindi non contano per il limite giornaliero
    private Match playRankedUntilSomeoneWins(User challenger, User opponent) {
        for (int attempt = 0; attempt < 20; attempt++) {
            Match match = playRanked(challenger, opponent);
            if (match.getWinner() != null) {
                return match;
            }
        }
        throw new AssertionError("Venti pareggi di fila: le carte di prova non vanno bene per questo test");
    }

    // Lo stesso mazzo di prova per tutti: due copie di carte di fazioni diverse
    private DeckCheck deck(User owner) {
        Deck deck = new Deck(owner, "Mazzo di prova");
        List<DeckCards> cards = new ArrayList<>();
        for (String code : List.of("TEST-001", "TEST-002", "TEST-014", "TEST-015")) {
            cards.add(new DeckCards(deck, cardRepository.findByCode(code).orElseThrow(), 2));
        }
        return new DeckCheck(deck, cards, List.of());
    }

    private static double challengerScore(Match match) {
        if (match.getWinner() == null) {
            return 0.5;
        }
        return match.getWinner().getId().equals(match.getChallenger().getId()) ? 1 : 0;
    }
}
