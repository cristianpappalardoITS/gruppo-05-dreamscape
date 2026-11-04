package it.somnia.dreamscape.api.card.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

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

// Una carta del catalogo (sempre una creatura) oppure una sua versione, es. la full art.
// La versione ha base_card_id e non ha dati propri: nome, testi, ATT, DIF, fazione e
// rarità sono null e si leggono dalla carta base (decisione 025, chk_cards_variant).
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Codice stabile, es. BASE-004; la full art aggiunge -FA (BASE-004-FA)
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    // Carta normale di cui questa è una versione; null per le carte normali
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_card_id")
    private Card baseCard;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", length = 20)
    private CardVariantType variantType;

    @Column(name = "name", length = 80)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "attack")
    private Integer attack;

    @Column(name = "defense")
    private Integer defense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction_id")
    private Faction faction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rarity_id")
    private Rarity rarity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_set_id")
    private CardSet cardSet;

    // Copia WebP grande (larghezza 1024 px), percorso relativo es. /images/cards/base-004-v1.webp
    @Column(name = "illustration_url", length = 500)
    private String illustrationUrl;

    // Miniatura WebP (larghezza 400 px); c'è sempre insieme alla copia grande
    @Column(name = "illustration_thumb_url", length = 500)
    private String illustrationThumbUrl;

    // Ritaglio verticale dell'illustrazione: 0 = in alto, 1 = in basso
    @Column(name = "illustration_focus_y", nullable = false, precision = 3, scale = 2)
    private BigDecimal illustrationFocusY = new BigDecimal("0.40");

    // Disattivata: non esce dai pacchetti e non si usa nei mazzi (decisione 026)
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
