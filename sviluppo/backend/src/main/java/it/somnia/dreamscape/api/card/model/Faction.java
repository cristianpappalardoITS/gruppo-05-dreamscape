package it.somnia.dreamscape.api.card.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Una fazione: Dreams, Nightmares o Memories, con il suo Lord (decisione 001)
@Entity
@Table(name = "factions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faction {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Codice stabile: DREAMS, NIGHTMARES, MEMORIES
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    // Titolo del Lord, es. "Dreams Lord"
    @Column(name = "lord_title", length = 50)
    private String lordTitle;

    // La Leggendaria normale che fa da Lord; null finché non c'è il catalogo vero
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lord_card_id")
    private Card lordCard;
}
