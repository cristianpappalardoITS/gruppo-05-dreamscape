package it.somnia.dreamscape.api.pack.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.card.model.Faction;
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

// Una copertina del pacchetto (es. una per Lord): il contenuto del pacchetto non cambia
@Entity
@Table(name = "pack_covers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackCover {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_id")
    private Pack pack;

    // La fazione del Lord in copertina; null se la copertina non è di una fazione
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction_id")
    private Faction faction;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
