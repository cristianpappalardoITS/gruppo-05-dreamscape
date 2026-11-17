package it.somnia.dreamscape.engine.server.config;

import it.somnia.dreamscape.engine.log.EventType;
import it.somnia.dreamscape.engine.log.GameEvent;
import it.somnia.dreamscape.engine.log.Phase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.boot.jackson.JacksonMixin;

/**
 * Aggiunge {@code phase} e {@code type} al JSON di ogni evento del log, all'inizio, come in
 * {@code docs/database/schema.md}.
 *
 * <p>Negli eventi del nucleo {@code type()} (e quasi sempre {@code phase()}) è un metodo e non un campo del
 * record, quindi Jackson da solo non lo scriverebbe. engine-core non può usare Jackson: le annotazioni
 * stanno qui.
 */
@JacksonMixin(GameEvent.class)
@JsonPropertyOrder({ "phase", "type" })
public interface GameEventMixin {

    @JsonProperty("phase")
    Phase phase();

    @JsonProperty("type")
    EventType type();
}
