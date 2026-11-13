package it.somnia.dreamscape.api.pack.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.card.model.CardSet;
import it.somnia.dreamscape.api.card.model.Faction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un pacchetto: in vendita nel negozio, set iniziale o pacchetto di benvenuto (decisione 012).
// Un pacchetto nuovo è una riga di dati: prezzo, slot e pesi cambiano senza toccare il codice.
@Entity
@Table(name = "packs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pack {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "pack_type", nullable = false, length = 20)
    private PackType packType = PackType.SHOP;

    // Prezzo in Dream Credits: maggiore di 0 solo per i pacchetti SHOP
    @Column(name = "price_credits", nullable = false)
    private int priceCredits;

    // Se c'è, il pacchetto pesca solo le carte di questa espansione; null = tutte le espansioni attive
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_set_id")
    private CardSet cardSet;

    // Se c'è, il pacchetto pesca solo le carte di questa fazione; null = tutte le fazioni
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction_id")
    private Faction faction;

    // Percentuale con cui una Leggendaria estratta esce nella sua full art, es. 20.00
    @Column(name = "full_art_chance", nullable = false, precision = 5, scale = 2)
    private BigDecimal fullArtChance = new BigDecimal("20.00");

    // Periodo di vendita; null = nessun limite da quel lato
    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "available_until")
    private Instant availableUntil;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // BatchSize: con più pacchetti nel negozio carica slot e copertine con poche query invece di una per pacchetto
    @OneToMany(mappedBy = "pack")
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<PackSlot> slots = new ArrayList<>();

    @OneToMany(mappedBy = "pack")
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<PackCover> covers = new ArrayList<>();

    /**
     * Dice se il pacchetto si può comprare in questo momento.
     *
     * <p>Deve essere di tipo SHOP, attivo e dentro il periodo di vendita: {@code available_from}
     * è compreso, {@code available_until} no.
     *
     * @param now il momento da controllare
     * @return {@code true} se il pacchetto è in vendita
     */
    public boolean isOnSale(Instant now) {
        return packType == PackType.SHOP
                && active
                && (availableFrom == null || !now.isBefore(availableFrom))
                && (availableUntil == null || now.isBefore(availableUntil));
    }
}
