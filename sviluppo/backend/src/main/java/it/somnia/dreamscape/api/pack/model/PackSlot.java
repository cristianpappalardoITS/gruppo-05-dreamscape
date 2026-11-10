package it.somnia.dreamscape.api.pack.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Uno slot del pacchetto: quante carte dà e con quali rarità possibili (es. 3 Comuni, oppure 1 carta speciale)
@Entity
@Table(name = "pack_slots")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackSlot {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_id")
    private Pack pack;

    // Ordine dello slot nel pacchetto, da 1
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "card_count", nullable = false)
    private int cardCount;

    @OneToMany(mappedBy = "slot")
    @BatchSize(size = 50)
    private List<PackSlotRarity> rarities = new ArrayList<>();
}
