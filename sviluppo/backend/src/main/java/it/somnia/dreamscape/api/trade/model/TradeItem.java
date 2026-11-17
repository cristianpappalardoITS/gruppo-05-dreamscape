package it.somnia.dreamscape.api.trade.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.card.model.Card;
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

// Una carta di uno scambio, da un lato. La full art è una carta a parte: si scambia con il suo card_id.
// Una sola riga per carta e lato (uq_trade_items_trade_card_side)
@Entity
@Table(name = "trade_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeItem {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private TradeSide side;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    TradeItem(Trade trade, Card card, TradeSide side, int quantity) {
        this.trade = trade;
        this.card = card;
        this.side = side;
        this.quantity = quantity;
    }
}
