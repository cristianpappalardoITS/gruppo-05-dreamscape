package it.somnia.dreamscape.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import it.somnia.dreamscape.api.match.service.InProcessBattleSimulator;

// Nei test le partite le gioca il motore dentro il backend (InProcessBattleSimulator): niente container del
// motore, e lo stesso contesto di Spring per tutti i test. HttpBattleSimulator ha i suoi test a parte
@TestConfiguration(proxyBeanMethods = false)
public class EngineTestConfiguration {

    @Bean
    @Primary
    InProcessBattleSimulator inProcessBattleSimulator() {
        return new InProcessBattleSimulator();
    }
}
