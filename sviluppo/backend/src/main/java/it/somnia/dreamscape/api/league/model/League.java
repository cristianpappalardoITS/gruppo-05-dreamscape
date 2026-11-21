package it.somnia.dreamscape.api.league.model;

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

// Una lega del rank (V10, decisione 011): ci si sta quando il punteggio raggiunge min_rating e non la soglia
// della lega successiva. Codice e nome non cambiano; soglia e premio si cambiano dal backoffice
@Entity
@Table(name = "leagues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class League {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "code", nullable = false, length = 30, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 40, updatable = false)
    private String name;

    // La lega più bassa parte da 0: è quella in cui si parte
    @Setter
    @Column(name = "min_rating", nullable = false)
    private int minRating;

    // Dream Credits la prima volta che si entra nella lega; 0 = nessun premio
    @Setter
    @Column(name = "reward_credits", nullable = false)
    private int rewardCredits;
}
