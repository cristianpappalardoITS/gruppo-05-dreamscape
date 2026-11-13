package it.somnia.dreamscape.api.common.config;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Un solo generatore casuale per tutto il backend (es. le carte dei pacchetti).
// SecureRandom: le estrazioni non si possono prevedere. Nei test si può sostituire con uno a seed fisso.
@Configuration
public class RandomConfig {

    @Bean
    public RandomGenerator randomGenerator() {
        return new SecureRandom();
    }
}
