package it.somnia.dreamscape.api.match.model;

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

// Una modalità di gioco con i suoi punti vita e il limite di round. Oggi c'è solo BASE (V6),
// usata da tutte le partite: cambia il tipo di partita, non le regole (decisione 010).
@Entity
@Table(name = "game_modes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameMode {

    // Codice della modalità usata da tutte le partite
    public static final String BASE_CODE = "BASE";

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Codice stabile, es. BASE
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "starting_life_points", nullable = false)
    private int startingLifePoints;

    @Column(name = "round_limit", nullable = false)
    private int roundLimit;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
