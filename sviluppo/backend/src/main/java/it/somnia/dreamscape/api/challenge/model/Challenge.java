package it.somnia.dreamscape.api.challenge.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.match.model.GameMode;
import it.somnia.dreamscape.api.user.model.User;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

// Una sfida tra amici (V6, decisione 010): chi sfida sceglie il suo mazzo, l'amico accetta scegliendo il suo.
// È sempre una partita rapida. Se un mazzo viene cancellato la colonna diventa null (ON DELETE SET NULL)
@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenger_id", nullable = false)
    private User challenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opponent_id", nullable = false)
    private User opponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenger_deck_id")
    private Deck challengerDeck;

    // Il mazzo scelto da chi accetta: c'è solo nelle sfide giocate
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_deck_id")
    private Deck opponentDeck;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_mode_id", nullable = false)
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChallengeStatus status = ChallengeStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Quando è stata giocata, rifiutata o annullata; null finché è in attesa
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Challenge(User challenger, User opponent, Deck challengerDeck, GameMode gameMode) {
        this.challenger = challenger;
        this.opponent = opponent;
        this.challengerDeck = challengerDeck;
        this.gameMode = gameMode;
    }

    public boolean isPending() {
        return status == ChallengeStatus.PENDING;
    }

    public boolean involves(UUID userId) {
        return challenger.getId().equals(userId) || opponent.getId().equals(userId);
    }

    // Rifiutata o annullata
    public void resolve(ChallengeStatus newStatus, Instant now) {
        this.status = newStatus;
        this.resolvedAt = now;
    }

    // Accettata e giocata con il mazzo scelto da chi ha accettato
    public void complete(Deck acceptedDeck, Instant now) {
        this.opponentDeck = acceptedDeck;
        resolve(ChallengeStatus.COMPLETED, now);
    }
}
