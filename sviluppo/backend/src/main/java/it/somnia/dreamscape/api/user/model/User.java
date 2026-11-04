package it.somnia.dreamscape.api.user.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.card.model.Faction;
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
import lombok.Setter;

// Un Dreamweaver (giocatore) o un admin. Gli utenti non si cancellano: si disattivano
// o si anonimizzano (decisione 018). Tabella e regole in docs/database/schema.md.
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    // Chiave UUID v7 generata da Hibernate prima del salvataggio
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Alla registrazione al massimo 30 caratteri; la colonna ne tiene 40 per l'account anonimizzato
    @Column(name = "username", nullable = false, length = 40)
    private String username;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.PLAYER;

    // Saldo in Dream Credits: sempre intero, mai negativo
    @Column(name = "credits", nullable = false)
    private int credits;

    // Punteggio del rank (Elo), si parte da 1000
    @Column(name = "rating", nullable = false)
    private int rating = 1000;

    // Fazione scelta nella pagina Benvenuto; null finché non si sceglie
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "starter_faction_id")
    private Faction starterFaction;

    // Mazzo classificato: per ora solo l'id, finché non c'è l'entità Deck (#80)
    @Column(name = "ranked_deck_id")
    private UUID rankedDeckId;

    // Giorno (ora italiana) dell'ultima ricompensa per l'accesso giornaliero
    @Column(name = "last_daily_reward_on")
    private LocalDate lastDailyRewardOn;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "privacy_accepted_at")
    private Instant privacyAcceptedAt;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
