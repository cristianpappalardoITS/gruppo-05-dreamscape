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

// "faction è forte contro targetFaction": il bonus vale per la creatura di faction
@Entity
@Table(name = "faction_advantages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FactionAdvantage {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faction_id")
    private Faction faction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_faction_id")
    private Faction targetFaction;

    @Column(name = "attack_bonus", nullable = false)
    private int attackBonus;

    @Column(name = "defense_bonus", nullable = false)
    private int defenseBonus;
}
