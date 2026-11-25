package it.somnia.dreamscape.api.mission.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.model.DeckCards;
import it.somnia.dreamscape.api.deck.service.DeckCheck;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.service.MatchService;
import it.somnia.dreamscape.api.mission.dto.response.MissionResponse;
import it.somnia.dreamscape.api.mission.model.MissionPeriod;
import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.CreditTransactionRepository;
import it.somnia.dreamscape.api.user.repository.UserRepository;

// Missioni (#125): le partite le gioca il motore dentro il backend con le carte di prova (V900), quindi il
// vincitore non si sceglie e, quando serve, si rigioca finché il risultato non è quello voluto. Le partite qui
// sono del matchmaking (senza sfida): conta solo chi le avvia
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MissionServiceTest {

    @Autowired
    private MissionService missionService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private GameProperties gameProperties;

    private User anna;
    private User bruno;

    @BeforeEach
    void setUp() {
        anna = userRepository.findByUsernameIgnoreCase("anna_test").orElseThrow();
        bruno = userRepository.findByUsernameIgnoreCase("bruno_test").orElseThrow();
    }

    @Test
    void missionsOf_noMatches_returnsDailyThenWeeklyAtZero() {
        // Given: nessuna partita giocata

        // When
        List<MissionResponse> missions = missionService.missionsOf(anna.getId());

        // Then
        assertThat(missions).extracting(MissionResponse::code).containsExactly("DAILY_PLAY", "WEEKLY_WIN");
        assertThat(missions).allSatisfy(mission -> {
            assertThat(mission.progress()).isZero();
            assertThat(mission.isCompleted()).isFalse();
        });
        assertThat(missions.get(0).goalCount()).isEqualTo(1);
        assertThat(missions.get(0).rewardCredits()).isEqualTo(100);
        assertThat(missions.get(1).goalCount()).isEqualTo(5);
        assertThat(missions.get(1).rewardCredits()).isEqualTo(250);
    }

    @Test
    void missionsOf_anyDay_resetsAtTheNextMidnightAndNextMonday() {
        // Given
        ZoneId zone = gameProperties.timeZone();
        LocalDate today = LocalDate.now(zone);

        // When
        List<MissionResponse> missions = missionService.missionsOf(anna.getId());

        // Then
        assertThat(missions.get(0).resetsAt()).isEqualTo(today.plusDays(1).atStartOfDay(zone).toInstant());
        LocalDate nextMonday = MissionPeriod.WEEKLY.nextStart(MissionPeriod.WEEKLY.startOf(today));
        assertThat(missions.get(1).resetsAt()).isEqualTo(nextMonday.atStartOfDay(zone).toInstant());
        assertThat(missions.get(1).resetsAt()).isAfter(Instant.now());
    }

    @Test
    void recordMatch_firstCasualMatchOfTheDay_completesTheDailyAndGivesTheReward() {
        // Given
        int credits = anna.getCredits();
        Match match = play(MatchType.CASUAL);

        // When
        missionService.recordMatch(match);

        // Then
        MissionResponse daily = mission(anna, "DAILY_PLAY");
        assertThat(daily.progress()).isEqualTo(1);
        assertThat(daily.isCompleted()).isTrue();
        assertThat(anna.getCredits()).isEqualTo(credits + 100);
        assertThat(missionRewards(anna)).singleElement().satisfies(reward -> {
            assertThat(reward.getAmount()).isEqualTo(100);
            assertThat(reward.getMatchId()).isEqualTo(match.getId());
        });
    }

    @Test
    void recordMatch_secondMatchOfTheDay_doesNotGiveTheRewardAgain() {
        // Given
        missionService.recordMatch(play(MatchType.CASUAL));
        int credits = anna.getCredits();

        // When
        missionService.recordMatch(play(MatchType.RANKED));

        // Then
        assertThat(mission(anna, "DAILY_PLAY").progress()).isEqualTo(1);
        assertThat(anna.getCredits()).isEqualTo(credits);
        assertThat(missionRewards(anna)).hasSize(1);
    }

    @Test
    void recordMatch_matchmakingOpponent_doesNotAdvance() {
        // Given: bruno viene scelto dal matchmaking, non ha avviato la partita
        int credits = bruno.getCredits();

        // When
        missionService.recordMatch(play(MatchType.CASUAL));

        // Then
        assertThat(mission(bruno, "DAILY_PLAY").progress()).isZero();
        assertThat(bruno.getCredits()).isEqualTo(credits);
    }

    @Test
    void recordMatch_casualWin_doesNotAdvanceTheWeekly() {
        // Given
        Match win = playUntil(MatchType.CASUAL, this::annaWins);

        // When
        missionService.recordMatch(win);

        // Then
        assertThat(mission(anna, "WEEKLY_WIN").progress()).isZero();
    }

    @Test
    void recordMatch_rankedLoss_doesNotAdvanceTheWeekly() {
        // Given
        Match loss = playUntil(MatchType.RANKED, match -> !annaWins(match));

        // When
        missionService.recordMatch(loss);

        // Then
        assertThat(mission(anna, "WEEKLY_WIN").progress()).isZero();
    }

    @Test
    void recordMatch_fifthRankedWin_completesTheWeeklyOnce() {
        // Given: quattro vittorie classificate già contate
        for (int i = 0; i < 4; i++) {
            missionService.recordMatch(playUntil(MatchType.RANKED, this::annaWins));
        }
        assertThat(mission(anna, "WEEKLY_WIN").progress()).isEqualTo(4);
        int credits = anna.getCredits();

        // When: la quinta e poi una sesta
        missionService.recordMatch(playUntil(MatchType.RANKED, this::annaWins));
        int creditsAfterFifth = anna.getCredits();
        missionService.recordMatch(playUntil(MatchType.RANKED, this::annaWins));

        // Then
        MissionResponse weekly = mission(anna, "WEEKLY_WIN");
        assertThat(weekly.progress()).isEqualTo(5);
        assertThat(weekly.isCompleted()).isTrue();
        assertThat(creditsAfterFifth).isEqualTo(credits + 250);
        assertThat(anna.getCredits()).isEqualTo(creditsAfterFifth);
        assertThat(missionRewards(anna)).extracting(CreditTransaction::getAmount).containsExactlyInAnyOrder(100, 250);
    }

    private MissionResponse mission(User user, String code) {
        return missionService.missionsOf(user.getId()).stream()
                .filter(mission -> mission.code().equals(code))
                .findFirst()
                .orElseThrow();
    }

    private List<CreditTransaction> missionRewards(User user) {
        return creditTransactionRepository.findAll().stream()
                .filter(transaction -> transaction.getUser().getId().equals(user.getId()))
                .filter(transaction -> transaction.getType() == CreditTransactionType.MISSION_REWARD)
                .toList();
    }

    private boolean annaWins(Match match) {
        return match.getWinner() != null && match.getWinner().getId().equals(anna.getId());
    }

    // anna avvia, bruno è l'avversario scelto dal matchmaking
    private Match play(MatchType matchType) {
        return matchService.play(matchType, anna, deck(anna), bruno, deck(bruno), null);
    }

    // Il motore decide il vincitore: si rigioca finché il risultato non è quello voluto. Le partite scartate non
    // passano dalle missioni, quindi non contano
    private Match playUntil(MatchType matchType, Predicate<Match> wanted) {
        for (int attempt = 0; attempt < 30; attempt++) {
            Match match = play(matchType);
            if (wanted.test(match)) {
                return match;
            }
        }
        throw new AssertionError("Trenta partite senza il risultato voluto: le carte di prova non vanno bene");
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
}
