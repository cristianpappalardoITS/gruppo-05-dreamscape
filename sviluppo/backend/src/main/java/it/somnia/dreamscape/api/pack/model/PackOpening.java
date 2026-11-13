package it.somnia.dreamscape.api.pack.model;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// L'apertura di un pacchetto, comprato o regalato, con le carte uscite.
// Ha il suo id: la pagina dell'apertura lo mette nell'URL, così ricaricarla non compra un altro pacchetto.
@Entity
@Table(name = "pack_openings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackOpening {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_id")
    private Pack pack;

    // La copertina scelta; null se il pacchetto non ne ha o se è stata cancellata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_cover_id")
    private PackCover cover;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private PackOpeningSource source;

    // Crediti pagati: maggiore di 0 solo per gli acquisti
    @Column(name = "price_paid", nullable = false)
    private int pricePaid;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    // Le carte nell'ordine in cui sono uscite: gli id UUID v7 crescono con l'inserimento
    @OneToMany(mappedBy = "opening", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<PackOpeningCard> cards = new ArrayList<>();

    public PackOpening(User user, Pack pack, PackCover cover, PackOpeningSource source, int pricePaid) {
        this.user = user;
        this.pack = pack;
        this.cover = cover;
        this.source = source;
        this.pricePaid = pricePaid;
    }

    /**
     * Aggiunge una carta uscita dal pacchetto, in coda alle altre.
     *
     * @param card la carta uscita: la carta normale oppure la sua full art
     */
    public void addCard(Card card) {
        cards.add(new PackOpeningCard(this, card));
    }
}
