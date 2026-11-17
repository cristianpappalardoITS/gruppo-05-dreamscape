package it.somnia.dreamscape.engine.server.config;

import it.somnia.dreamscape.engine.battle.BattleEngine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Il motore del nucleo come bean: {@link BattleEngine} non ha stato, quindi ne basta uno per tutte le richieste.
 */
@Configuration
public class EngineConfig {

    @Bean
    public BattleEngine battleEngine() {
        return new BattleEngine();
    }
}
