package it.somnia.dreamscape.engine.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Avvio di engine-service, il servizio che espone il motore di gioco al backend (decisione 029).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EngineServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineServiceApplication.class, args);
    }
}
