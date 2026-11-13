package it.somnia.dreamscape.api.pack.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.card.model.Card;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Una carta uscita da un'apertura: card_id è la carta normale o la sua full art
@Entity
@Table(name = "pack_opening_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackOpeningCard {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_opening_id")
    private PackOpening opening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    PackOpeningCard(PackOpening opening, Card card) {
        this.opening = opening;
        this.card = card;
    }
}
