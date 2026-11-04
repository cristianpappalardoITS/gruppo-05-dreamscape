package it.somnia.dreamscape.api.card.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un TIER di rarità con i limiti del mazzo (decisione 017)
@Entity
@Table(name = "rarities")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rarity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Codice stabile: COMMON, RARE, EPIC, LEGENDARY
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // Quante carte di questa rarità al massimo in un mazzo; null = nessun limite
    @Column(name = "max_per_deck")
    private Integer maxPerDeck;

    // Quante copie della stessa carta al massimo in un mazzo
    @Column(name = "max_copies", nullable = false)
    private int maxCopies = 2;
}
