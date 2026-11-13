package it.somnia.dreamscape.api.pack.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.card.model.Rarity;
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

// Una rarità possibile in uno slot, con il suo peso.
// Probabilità della rarità = peso / somma dei pesi dello slot.
@Entity
@Table(name = "pack_slot_rarities")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackSlotRarity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_slot_id")
    private PackSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rarity_id")
    private Rarity rarity;

    @Column(name = "weight", nullable = false)
    private int weight;
}
