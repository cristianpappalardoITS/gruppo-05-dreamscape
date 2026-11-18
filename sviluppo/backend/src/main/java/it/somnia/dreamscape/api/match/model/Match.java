package it.somnia.dreamscape.api.match.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.challenge.model.Challenge;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.engine.EndReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Una partita giocata dal motore (V6, decisioni 010 e 029). challenger è chi l'ha avviata (chi ha lanciato la
// sfida), opponent l'avversario; winner null = pareggio. Le copie dei mazzi e il log sono JSON (schema.md,
// "Formato dei campi JSON di matches"): si salvano e si leggono sempre interi
@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 10)
    private MatchType matchType;

    // Solo per le sfide tra amici
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenger_id", nullable = false)
    private User challenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opponent_id", nullable = false)
    private User opponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", nullable = false, length = 30)
    private EndReason endReason;

    @Column(name = "total_rounds", nullable = false)
    private int totalRounds;

    // Copia dei valori della modalità usati in questa partita
    @Column(name = "starting_life_points", nullable = false)
    private int startingLifePoints;

    @Column(name = "round_limit", nullable = false)
    private int roundLimit;

    @Column(name = "challenger_life_points", nullable = false)
    private int challengerLifePoints;

    @Column(name = "opponent_life_points", nullable = false)
    private int opponentLifePoints;

    // Variazione del punteggio di rank: null nelle partite rapide (chk_matches_rating)
    @Column(name = "challenger_rating_change")
    private Integer challengerRatingChange;

    @Column(name = "opponent_rating_change")
    private Integer opponentRatingChange;

    // Con lo stesso seed e gli stessi mazzi il motore rigioca la stessa partita
    @Column(name = "random_seed", nullable = false)
    private long randomSeed;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "challenger_deck_snapshot", nullable = false, columnDefinition = "jsonb")
    private String challengerDeckSnapshot;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "opponent_deck_snapshot", nullable = false, columnDefinition = "jsonb")
    private String opponentDeckSnapshot;

    // Il log del motore così com'è: versione del motore, moneta e round per round
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "round_log", nullable = false, columnDefinition = "jsonb")
    private String roundLog;

    @CreationTimestamp
    @Column(name = "played_at", nullable = false, updatable = false)
    private Instant playedAt;

    @Builder
    private Match(MatchType matchType, Challenge challenge, User challenger, User opponent, User winner,
            EndReason endReason, int totalRounds, int startingLifePoints, int roundLimit, int challengerLifePoints,
            int opponentLifePoints, Integer challengerRatingChange, Integer opponentRatingChange, long randomSeed,
            String challengerDeckSnapshot, String opponentDeckSnapshot, String roundLog) {
        this.matchType = matchType;
        this.challenge = challenge;
        this.challenger = challenger;
        this.opponent = opponent;
        this.winner = winner;
        this.endReason = endReason;
        this.totalRounds = totalRounds;
        this.startingLifePoints = startingLifePoints;
        this.roundLimit = roundLimit;
        this.challengerLifePoints = challengerLifePoints;
        this.opponentLifePoints = opponentLifePoints;
        this.challengerRatingChange = challengerRatingChange;
        this.opponentRatingChange = opponentRatingChange;
        this.randomSeed = randomSeed;
        this.challengerDeckSnapshot = challengerDeckSnapshot;
        this.opponentDeckSnapshot = opponentDeckSnapshot;
        this.roundLog = roundLog;
    }

    public boolean involves(UUID userId) {
        return challenger.getId().equals(userId) || opponent.getId().equals(userId);
    }
}
