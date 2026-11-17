package it.somnia.dreamscape.api.trade.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.user.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Uno scambio tra due amici (V5, decisioni 009 e 024). Le copie offerte di uno scambio PENDING sono bloccate
// per gli altri scambi di chi propone: si contano dalle righe di trade_items, nessuna colonna in più
@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TradeStatus status = TradeStatus.PENDING;

    // Messaggio facoltativo di chi propone
    @Column(name = "message", length = 255)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Quando è stato accettato, rifiutato o annullato; null finché è in attesa
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @OneToMany(mappedBy = "trade", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeItem> items = new ArrayList<>();

    public Trade(User proposer, User recipient, String message) {
        this.proposer = proposer;
        this.recipient = recipient;
        this.message = message;
    }

    public void addItem(Card card, TradeSide side, int quantity) {
        items.add(new TradeItem(this, card, side, quantity));
    }

    public List<TradeItem> itemsOf(TradeSide side) {
        return items.stream().filter(item -> item.getSide() == side).toList();
    }

    public boolean isPending() {
        return status == TradeStatus.PENDING;
    }

    public boolean involves(UUID userId) {
        return proposer.getId().equals(userId) || recipient.getId().equals(userId);
    }

    // Chiude lo scambio: da qui le copie offerte non sono più bloccate
    public void resolve(TradeStatus newStatus, Instant now) {
        this.status = newStatus;
        this.resolvedAt = now;
    }
}
