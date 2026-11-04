package it.somnia.dreamscape.api.card.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// L'effetto di una carta normale, con il suo valore X (al massimo un effetto per carta)
@Entity
@Table(name = "card_effects")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardEffect {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "effect_id")
    private Effect effect;

    // Valore X; null per gli effetti senza numero (es. Neutralità)
    @Column(name = "effect_value")
    private Integer effectValue;
}
