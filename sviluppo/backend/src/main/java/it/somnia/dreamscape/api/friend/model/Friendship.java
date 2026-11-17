package it.somnia.dreamscape.api.friend.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

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

// Una riga della lista amici (V5, decisione 009): A (requester) manda la richiesta, B (addressee) la accetta.
// Una sola riga per coppia, in qualunque direzione: lo garantisce l'indice uq_friendships_pair
@Entity
@Table(name = "friendships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Solo per le amicizie accettate (vincolo chk_friendships_accepted)
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
    }

    public void accept(Instant now) {
        this.status = FriendshipStatus.ACCEPTED;
        this.acceptedAt = now;
    }

    public boolean isAccepted() {
        return status == FriendshipStatus.ACCEPTED;
    }

    // L'altro giocatore della coppia, visto da userId
    public User otherThan(UUID userId) {
        return requester.getId().equals(userId) ? addressee : requester;
    }
}
