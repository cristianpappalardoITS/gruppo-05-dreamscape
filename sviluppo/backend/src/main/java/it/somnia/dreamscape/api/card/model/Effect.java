package it.somnia.dreamscape.api.card.model;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un effetto del catalogo provvisorio (decisione 028). Il comportamento sta nel motore:
// qui ci sono solo il codice, il testo per i giocatori e quando agisce.
@Entity
@Table(name = "effects")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Effect {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    // Collega l'effetto alla sua classe nel motore, es. PUNISHMENT
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    // Testo per i giocatori; {value} si sostituisce con il valore X della carta
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false, length = 20)
    private EffectTrigger triggerEvent;

    // TRUE se l'effetto ha il valore X (es. "Castigo 2")
    @Getter(AccessLevel.NONE)
    @Column(name = "has_value", nullable = false)
    private boolean hasValue;

    // Spento: non si può più assegnare alle carte
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public boolean hasValue() {
        return hasValue;
    }
}
